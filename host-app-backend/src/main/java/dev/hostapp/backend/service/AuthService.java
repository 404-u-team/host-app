package dev.hostapp.backend.service;

import dev.hostapp.backend.exceptions.UnauthorizedException;
import dev.hostapp.backend.exceptions.UserAlreadyExistsException;
import dev.hostapp.backend.exceptions.UserNotFoundException;
import dev.hostapp.backend.model.Device;
import dev.hostapp.backend.model.User;
import dev.hostapp.backend.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;


@Service
public class AuthService {
    private final UserService userService;
    private final DeviceRepository deviceRepository;
    private final EmailService emailService;
    private final EmailVerificationService verification;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public AuthService(UserService userService, DeviceRepository deviceRepository,
                       EmailService emailService, EmailVerificationService verification) {
        this.emailService = emailService;
        this.verification = verification;
        this.userService = userService;
        this.deviceRepository = deviceRepository;
    }

    @Transactional
    public User registerUser(String name, String surname, String email, String password) {
        if (name == null || name.isBlank() || surname == null || surname.isBlank()
                || email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Bad request");
        }
        email = email.trim();
        if (!EmailService.isEmailAddress(email)) {
            throw new IllegalArgumentException("Неверный email");
        }
        if (name.trim().length() > 255 || surname.trim().length() > 255 || password.length() > 255) {
            throw new IllegalArgumentException("Поля регистрации: не более 255 символов");
        }
        if (userService.emailExists(email)) {
            User existing = userService.getByEmail(email);
            if (!existing.isEmailVerified()
                    && Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8().matches(password, existing.getPasswordHash())) {
                return existing; // Resume verification without replacing credentials or bypassing resend limits.
            }
            throw new UserAlreadyExistsException("User already exists");
        }
        PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

        User user = userService.createUser(
            name.trim(),
            surname.trim(),
            email,
            encoder.encode(password)
        );
        if (emailService.isVerificationRequired()) {
            user.requireEmailVerification();
            verification.sendCode(user);
        }
        return user;
    }

    private User authenticate(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Bad request");
        }
        PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

        User user;
        try {
            user = userService.getByEmail(email);
        } catch (UserNotFoundException exception) {
            throw new UnauthorizedException("Incorrect email or password");
        }

        if (!encoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("Incorrect email or password");
        }
        if (!user.isEmailVerified()) {
            throw new UnauthorizedException("Подтвердите email. Откройте регистрацию с теми же email и паролем для ввода или повторной отправки кода.");
        }
        return user;
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
        User user = authenticate(email == null ? null : email.trim(), password);

        String token = generateNewToken();

        Device device = new Device(user, ip, userAgent, hashToken(token));
        deviceRepository.save(device);

        return token;
    }

    public User getUserByToken(String token) {
        String tokenHash = hashToken(token);

        Device device = deviceRepository.findByTokenHash(tokenHash).orElse(null);
        if (device == null || device.getSessionValidUntil().isBefore(Instant.now())) {
            return null;
        }

        return device.getUser().isEmailVerified() ? device.getUser() : null;
    }
}
