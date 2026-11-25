package dev.hostapp.desktop.service;

import dev.hostapp.desktop.api.AuthApi;
import dev.hostapp.desktop.dto.auth.LoginResponse;
import dev.hostapp.desktop.dto.auth.RegisterResponse;
import dev.hostapp.desktop.util.Validation;

import java.io.IOException;

public class AuthService {

    private final AuthApi api;

    public AuthService(AuthApi api) {
        this.api = api;
    }

    public LoginResponse login(String email, String password) throws IOException, InterruptedException {
        return api.login(Validation.email(email), Validation.password(password));
    }

    public RegisterResponse register(String name, String surname, String email, String password)
            throws IOException, InterruptedException {
        return api.register(Validation.text(name, "Имя", 255), Validation.text(surname, "Фамилия", 255),
                Validation.email(email), Validation.password(password));
    }
    public RegisterResponse verifyEmail(String email, String code) throws IOException, InterruptedException {
        if (code == null || !code.trim().matches("[0-9]{6}")) {
            throw new IllegalArgumentException("Введите 6 цифр из письма.");
        }
        return api.verifyEmail(email, code.trim());
    }

    public void resendEmailCode(String email, String password) throws IOException, InterruptedException {
        api.resendEmailCode(email, password);
    }
}
