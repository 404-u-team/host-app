package dev.hostapp.backend.service;

import dev.hostapp.backend.domain.Role;
import dev.hostapp.backend.dto.api.AccountDtos.*;
import dev.hostapp.backend.exceptions.ResourceNotFoundException;
import dev.hostapp.backend.exceptions.UserAlreadyExistsException;
import dev.hostapp.backend.filter.ApiQuery;
import dev.hostapp.backend.domain.User;
import dev.hostapp.backend.repository.DeviceRepository;
import dev.hostapp.backend.repository.UserRepository;
import dev.hostapp.backend.security.Passwords;
import dev.hostapp.backend.security.Tokens;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {
    private final UserRepository users;
    private final DeviceRepository devices;
    private final AuditService audit;
    private final EmailService email;

    public AdminUserService(UserRepository users, DeviceRepository devices, AuditService audit, EmailService email) {
        this.users = users;
        this.devices = devices;
        this.audit = audit;
        this.email = email;
    }

    @Transactional(readOnly = true)
    public Page<UserView> list(int page, int size) {
        return users.findAll(ApiQuery.page(page, size, Sort.by("email", "id"))).map(UserView::from);
    }

    @Transactional
    public UserView create(User actor, CreateUser request) {
        UserInput input = request.account();
        String login = validateLogin(input.login(), null);
        String temporary = temporaryPassword(request.temporaryPassword(), login);
        String hash = Passwords.encode(temporary);
        User user = new User(input.name().trim(), input.surname().trim(), login, hash);
        user.updateAccount(input.name().trim(), input.surname().trim(), login, input.roles(), input.active());
        user.setPassword(hash, true);
        users.saveAndFlush(user);
        audit.record(actor, "USER_CREATED", "USER", user.getId(), "Учётная запись создана");
        if (request.temporaryPassword() == null) deliver(login, temporary);
        return UserView.from(user);
    }

    @Transactional
    public UserView update(User actor, UUID id, UserInput input) {
        User user = locked(id);
        if (actor.getId().equals(id) && (!input.active() || !input.roles().contains(Role.ADMIN))) {
            throw new IllegalStateException("Администратор не может отключить себя или отозвать у себя роль администратора");
        }
        long oldVersion = user.getAuthVersion();
        user.updateAccount(input.name().trim(), input.surname().trim(), validateLogin(input.login(), user),
                input.roles(), input.active());
        if (oldVersion != user.getAuthVersion()) {
            devices.deleteByUser_Id(id);
        }
        users.flush();
        audit.record(actor, "USER_UPDATED", "USER", id, "Учётная запись изменена");
        return UserView.from(user);
    }

    @Transactional
    public UserView resetPassword(User actor, UUID id, ResetPassword request) {
        User user = locked(id);
        String supplied = request == null ? null : request.temporaryPassword();
        String temporary = temporaryPassword(supplied, user.getEmail());
        if (Passwords.matches(temporary, user.getPasswordHash())) {
            throw new IllegalArgumentException("Временный пароль должен отличаться от текущего");
        }
        user.setPassword(Passwords.encode(temporary), true);
        devices.deleteByUser_Id(id);
        users.flush();
        audit.record(actor, "PASSWORD_RESET", "USER", id, "Установлен временный пароль");
        if (supplied == null) deliver(user.getEmail(), temporary);
        return UserView.from(user);
    }

    private String temporaryPassword(String supplied, String login) {
        if (supplied != null) {
            Passwords.validateNew(supplied);
            return supplied;
        }
        if (!email.isConfigured() || !EmailService.isEmailAddress(login)) {
            throw new IllegalArgumentException("Укажите временный пароль. Для автоматической доставки нужен SMTP и email пользователя");
        }
        return Tokens.random();
    }

    private void deliver(String login, String password) {
        email.send(login, "Host App: временный пароль",
                "Ваш временный пароль: " + password + "\nПосле входа сразу смените его.");
    }

    private User locked(UUID id) {
        return users.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    private String validateLogin(String input, User existing) {
        String login = input.trim().toLowerCase(Locale.ROOT);
        if (existing != null && existing.getEmail().equalsIgnoreCase(login)) {
            return existing.getEmail();
        }
        if (!EmailService.isEmailAddress(login)) {
            throw new IllegalArgumentException("Логин должен быть адресом email");
        }
        if (users.existsByEmailIgnoreCase(login)) {
            throw new UserAlreadyExistsException("Логин уже используется");
        }
        return login;
    }
}
