package dev.hostapp.desktop.service;

import dev.hostapp.desktop.api.AuthApi;
import dev.hostapp.desktop.dto.auth.LoginResponse;
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

    public void register(String name, String surname, String email, String password)
            throws IOException, InterruptedException {
        api.register(Validation.text(name, "Имя", 255), Validation.text(surname, "Фамилия", 255),
                Validation.email(email), Validation.password(password));
    }
}
