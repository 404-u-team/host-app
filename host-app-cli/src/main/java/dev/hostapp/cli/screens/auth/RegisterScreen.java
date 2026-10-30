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
            while (response.verificationRequired()) {
                System.out.println("Подтвердите email: " + response.email() + ". Код действует 10 минут.");
                String code = readLine("Код (6 цифр), resend — отправить повторно, Enter — выйти: ");
                if (code.isBlank()) {
                    System.out.println("Для продолжения откройте регистрацию с теми же email и паролем.");
                    return;
                }
                try {
                    if (code.equalsIgnoreCase("resend")) {
                        authApi.resendEmailCode(response.email(), password);
                        System.out.println("Новый код отправлен. Повторная отправка доступна через 60 секунд.");
                    } else {
                        response = authApi.verifyEmail(response.email(), code.trim());
                    }
                } catch (Exception exception) {
                    printError(exception.getMessage());
                }
            }
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
