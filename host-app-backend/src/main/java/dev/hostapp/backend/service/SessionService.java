package dev.hostapp.backend.service;

import dev.hostapp.backend.domain.RefreshSession;
import dev.hostapp.backend.dto.api.AccountDtos.*;
import dev.hostapp.backend.exceptions.UnauthorizedException;
import dev.hostapp.backend.domain.User;
import dev.hostapp.backend.repository.DeviceRepository;
import dev.hostapp.backend.repository.RefreshSessionRepository;
import dev.hostapp.backend.repository.UserRepository;
import dev.hostapp.backend.security.JwtService;
import dev.hostapp.backend.security.Passwords;
import dev.hostapp.backend.security.Tokens;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {
    private final AuthService auth;
    private final RefreshSessionRepository sessions;
    private final UserRepository users;
    private final DeviceRepository devices;
    private final AuditService audit;
    private final JwtService jwt;
    private final int refreshDays;

    public SessionService(AuthService auth, RefreshSessionRepository sessions, UserRepository users,
                          DeviceRepository devices, AuditService audit, JwtService jwt,
                          @Value("${app.jwt.refresh-days}") int refreshDays) {
        if (refreshDays < 1 || refreshDays > 90) {
            throw new IllegalArgumentException("Срок refresh-сессии должен быть от 1 до 90 дней");
        }
        this.auth = auth;
        this.sessions = sessions;
        this.users = users;
        this.devices = devices;
        this.audit = audit;
        this.jwt = jwt;
        this.refreshDays = refreshDays;
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public TokenPair login(Login input, String ip) {
        User user = auth.authenticate(input.login(), input.password(), ip);
        return newSession(user);
    }

    @Transactional
    public TokenPair refresh(String token) {
        RefreshSession session = sessions.findForUpdate(Tokens.hash(token))
                .orElseThrow(SessionService::invalid);
        User user = session.getUser();
        if (!session.getExpiresAt().isAfter(Instant.now()) || session.getAuthVersion() != user.getAuthVersion()
                || !user.isActive() || !user.isEmailVerified()) {
            throw invalid();
        }
        String refresh = Tokens.random();
        session.rotate(Tokens.hash(refresh));
        return tokens(user, refresh);
    }

    @Transactional
    public void logout(String token) {
        sessions.findForUpdate(Tokens.hash(token)).ifPresent(session -> {
            User user = session.getUser();
            audit.record(user, "LOGOUT", "USER", user.getId(), "Refresh-сессия отозвана");
            sessions.delete(session);
        });
    }

    @Transactional
    public TokenPair changePassword(User actor, PasswordChange input) {
        User user = users.findByIdForUpdate(actor.getId()).orElseThrow(SessionService::invalid);
        if (!user.isActive() || actor.getAuthVersion() != user.getAuthVersion()
                || !Passwords.matches(input.currentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Неверный текущий пароль или устаревшая сессия");
        }
        Passwords.validateNew(input.newPassword());
        if (input.newPassword().equals(input.currentPassword())) {
            throw new IllegalArgumentException("Новый пароль должен отличаться от текущего");
        }
        user.setPassword(Passwords.encode(input.newPassword()), false);
        devices.deleteByUser_Id(user.getId());
        audit.record(user, "PASSWORD_CHANGED", "USER", user.getId(), "Пароль изменён");
        return newSession(user);
    }

    private TokenPair newSession(User user) {
        String refresh = Tokens.random();
        sessions.save(new RefreshSession(user, Tokens.hash(refresh), Instant.now().plus(refreshDays, ChronoUnit.DAYS)));
        return tokens(user, refresh);
    }

    private TokenPair tokens(User user, String refresh) {
        return new TokenPair(jwt.issue(user), refresh, jwt.expiresInSeconds(), user.mustChangePassword());
    }

    private static UnauthorizedException invalid() {
        return new UnauthorizedException("Refresh-сессия недействительна");
    }
}
