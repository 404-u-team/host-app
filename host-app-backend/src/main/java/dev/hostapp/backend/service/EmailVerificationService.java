package dev.hostapp.backend.service;

import dev.hostapp.backend.exceptions.UnauthorizedException;
import dev.hostapp.backend.model.User;
import dev.hostapp.backend.repository.UserRepository;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;

@Service
public class EmailVerificationService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    private final UserRepository users;
    private final EmailService email;

    public EmailVerificationService(UserRepository users, EmailService email) {
        this.users = users;
        this.email = email;
    }

    // Called inside the registration/resend transaction; delivery failure rolls it back.
    public void sendCode(User user) {
        Instant now = Instant.now();
        if (user.getEmailCodeSentAt() != null && user.getEmailCodeSentAt().plusSeconds(60).isAfter(now)) {
            throw new IllegalArgumentException("Повторная отправка доступна через 60 секунд после предыдущей");
        }
        String code = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
        user.issueEmailCode(encoder.encode(code), now);
        users.save(user);
        email.send(user.getEmail(), "Host App: подтверждение email",
                "Код подтверждения: " + code + "\nКод действует 10 минут. Никому не сообщайте его.");
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public User verify(String address, String code) {
        User user = findLocked(address);
        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("Email уже подтверждён");
        }
        if (user.getEmailCodeHash() == null || user.getEmailCodeExpiresAt() == null
                || !user.getEmailCodeExpiresAt().isAfter(Instant.now())) {
            throw new IllegalArgumentException("Код истёк. Запросите новый код");
        }
        if (user.getEmailCodeAttempts() >= 5) {
            throw new IllegalArgumentException("Попытки исчерпаны. Запросите новый код");
        }
        user.recordEmailCodeAttempt();
        if (code == null || !code.matches("[0-9]{6}") || !encoder.matches(code, user.getEmailCodeHash())) {
            throw new IllegalArgumentException("Неверный код подтверждения");
        }
        user.verifyEmail();
        return user;
    }

    @Transactional
    public User resend(String address, String password) {
        User user = findLocked(address);
        if (password == null || !encoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("Incorrect email or password");
        }
        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("Email уже подтверждён");
        }
        sendCode(user);
        return user;
    }

    private User findLocked(String address) {
        String value = address == null ? "" : address.trim();
        return users.findByEmailForUpdate(value)
                .orElseThrow(() -> new IllegalArgumentException("Неверный email или код подтверждения"));
    }
}
