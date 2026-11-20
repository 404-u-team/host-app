package dev.hostapp.backend.service;

import dev.hostapp.backend.exceptions.UnauthorizedException;
import dev.hostapp.backend.exceptions.UserAlreadyExistsException;
import dev.hostapp.backend.domain.Device;
import dev.hostapp.backend.domain.User;
import dev.hostapp.backend.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.hostapp.backend.repository.UserRepository;
import dev.hostapp.backend.security.LoginAttempts;
import dev.hostapp.backend.security.Passwords;
import dev.hostapp.backend.security.Tokens;
import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {
    private final UserService userService;
    private final DeviceRepository deviceRepository;
    private final EmailService emailService;
    private final EmailVerificationService verification;
    private final UserRepository users;
    private final LoginAttempts attempts;
    private final AuditService audit;

    public AuthService(UserService userService, DeviceRepository deviceRepository,
                       EmailService emailService, EmailVerificationService verification,
                       UserRepository users, LoginAttempts attempts, AuditService audit) {
        this.users = users;
        this.attempts = attempts;
        this.audit = audit;
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
                    && Passwords.matches(password, existing.getPasswordHash())) {
                return existing; // Resume verification without replacing credentials or bypassing resend limits.
            }
            throw new UserAlreadyExistsException("User already exists");
        }

        User user = userService.createUser(
            name.trim(),
            surname.trim(),
            email.toLowerCase(Locale.ROOT),
            Passwords.encode(password)
        );
        if (emailService.isVerificationRequired()) {
            user.requireEmailVerification();
            verification.sendCode(user);
        }
        audit.record(user, "USER_REGISTERED", "USER", user.getId(), "Пользователь зарегистрировался");
        return user;
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public User authenticate(String login, String password, String ip) {
        if (login == null || login.isBlank() || login.length() > 255 || password == null
                || password.isBlank() || password.length() > 255) {
            throw new IllegalArgumentException("Введите логин и пароль");
        }
        login = login.trim();
        attempts.start(login, ip);
        User user = users.lockLogin(login).orElse(null);
        if (!Passwords.matches(password, user == null ? null : user.getPasswordHash())
                || user == null || !user.isActive()) {
            audit.record(user, "LOGIN_FAILED", "USER", user == null ? null : user.getId(), "Вход отклонён");
            throw new UnauthorizedException("Неверный логин или пароль");
        }
        attempts.success(login, ip);
        if (!user.isEmailVerified()) {
            audit.record(user, "LOGIN_FAILED", "USER", user.getId(), "Email ещё не подтверждён");
            throw new UnauthorizedException("Подтвердите email. Откройте регистрацию с теми же email и паролем для ввода или повторной отправки кода.");
        }
        if (user.getPasswordHash().startsWith("$argon2")) {
            user.upgradePasswordHash(Passwords.encode(password));
        }
        audit.record(user, "LOGIN", "USER", user.getId(), "Пользователь вошёл в систему");
        return user;
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public String loginUser(String email, String password, String ip, String userAgent) {
        User user = authenticate(email, password, ip);
        String token = Tokens.random();
        String agent = userAgent == null || userAgent.length() <= 255 ? userAgent : userAgent.substring(0, 255);
        deviceRepository.save(new Device(user, ip, agent, Tokens.hash(token)));
        return token;
    }

    @Transactional(readOnly = true)
    public User getUserByToken(String token) {
        Device device = deviceRepository.findByTokenHash(Tokens.hash(token)).orElse(null);
        if (device == null || !device.getSessionValidUntil().isAfter(Instant.now())) {
            return null;
        }
        User user = device.getUser();
        return user.isEmailVerified() && user.isActive() ? user : null;
    }
}
