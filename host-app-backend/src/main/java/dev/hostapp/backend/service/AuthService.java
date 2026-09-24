package dev.hostapp.backend.service;

import dev.hostapp.backend.exception.UnauthorizedException;
import dev.hostapp.backend.exception.UserNotFoundException;
import dev.hostapp.backend.model.Device;
import dev.hostapp.backend.model.User;
import dev.hostapp.backend.repository.DeviceRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;


@Service
public class AuthService {
    private final UserService userService;
    private final DeviceRepository deviceRepository;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public AuthService(UserService userService, DeviceRepository deviceRepository) {
        this.userService = userService;
        this.deviceRepository = deviceRepository;
    }

    public User registerUser(String name, String surname, String email, String password) {
        if (name.isBlank() || surname.isBlank() || email.isBlank() || password.isBlank()) {
            throw new IllegalArgumentException("Bad request");
        }
        PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

        return userService.createUser(
            name,
            surname,
            email,
            encoder.encode(password)
        );
    }

    private boolean checkLogin(String email, String password) {
        if (email.isBlank() || password.isBlank()) {
            throw new IllegalArgumentException("Bad request");
        }
        PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

        User user;

            try {
                user = userService.getByEmail(email);
            } catch (UserNotFoundException exception) {
                return false;
            }

        if (!encoder.matches(password, user.getPasswordHash())) {
            return false;
        }
        return true;
    }

    private String generateNewToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public String loginUser(String email, String password, String ip, String userAgent) {
        if (!this.checkLogin(email, password)) {
            throw new UnauthorizedException("Incorrect email or password");
        }
        User user = userService.getByEmail(email);

        String token = generateNewToken();

        Device device = new Device(user, ip, userAgent, hashToken(token));
        deviceRepository.save(device);

        return token;
    }

    public User getUserByToken(String token) {
        String tokenHash = hashToken(token);

        Optional<Device> device = deviceRepository.findByTokenHash(tokenHash);

        if (!device.isPresent()) {
            return null;
        }

        if (device.get().getSessionValidUntil().isBefore(Instant.now())) {
            return null;
        }

        return device.get().getUser();
    }
}
