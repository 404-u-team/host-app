package dev.hostapp.backend.service;

import dev.hostapp.backend.model.Device;
import dev.hostapp.backend.model.User;
import dev.hostapp.backend.repository.DeviceRepository;
import dev.hostapp.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;


// TODO: разнести UserService на AuthService (управление регистрацией, входом и авторизацией) и UserService (чистое управление юзерами)
@Service
public class UserService {
    private final UserRepository repository;
    private final DeviceRepository deviceRepository;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public UserService(UserRepository userRepository, DeviceRepository deviceRepository) {
            this.repository = userRepository;
            this.deviceRepository = deviceRepository;
        }

    public User getByEmail(String email) {
        final Optional<User> user = repository.findByEmail(email);
        if (!user.isPresent()) {
            throw new RuntimeException("User not found");
        }
        return user.get();
    }

    public User registerUser(String name, String surname, String email, String password) {
        if (name.isBlank() || surname.isBlank() || email.isBlank() || password.isBlank()) {
            throw new IllegalArgumentException("Bad request");
        }
        PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

        User user = new User(name, surname, email, encoder.encode(password));

        return repository.save(user);
    }

    private boolean checkLogin(String email, String password) {
        if (email.isBlank() || password.isBlank()) {
            throw new IllegalArgumentException("Bad request");
        }
        PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

        User user = this.getByEmail(email);

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
            throw new IllegalArgumentException("Incorrect email or password");
        };
        User user = this.getByEmail(email);

        String token = generateNewToken();

        Device device = new Device(user, ip, userAgent, hashToken(token));
        deviceRepository.save(device);

        return token;
    }

    public List<User> getAll() {
        return repository.findAll();
    }
}
