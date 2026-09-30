package dev.hostapp.cli.screens.auth;

import dev.hostapp.cli.api.AuthApi;
import dev.hostapp.cli.dto.auth.RegisterResponse;
import dev.hostapp.cli.screens.BaseScreen;

import java.util.Scanner;

public class RegisterScreen extends BaseScreen {

    private final AuthApi authApi;

    public RegisterScreen(
        Scanner scanner,
        AuthApi authApi
    ) {
        super(scanner);
        this.authApi = authApi;
    }

    @Override
    public void show() {
        System.out.println();
        System.out.println("=== Регистрация ===");

        String name = readLine("Имя: ");
        String surname = readLine("Фамилия: ");
        String email = readLine("Email: ");
        String password = readLine("Пароль: ");

        try {
            RegisterResponse response = authApi.register(
                name,
                surname,
                email,
                password
            );

            System.out.println();
            System.out.println("Регистрация выполнена");

            System.out.println(
                "User: "
                    + response.name()
                    + " "
                    + response.surname()
            );

            System.out.println(
                "Email: " + response.email()
            );

        } catch (Exception e) {
            printError(e.getMessage());
        }
    }
}
