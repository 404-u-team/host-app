package dev.hostapp.cli.screens.auth;

import dev.hostapp.cli.api.AuthApi;
import dev.hostapp.cli.dto.auth.LoginResponse;
import dev.hostapp.cli.screens.BaseScreen;

import java.util.Scanner;

public class LoginScreen extends BaseScreen {

    private final AuthApi authApi;

    public LoginScreen(
        Scanner scanner,
        AuthApi authApi
    ) {
        super(scanner);
        this.authApi = authApi;
    }

    @Override
    public void show() {
        System.out.println();
        System.out.println("=== Login ===");

        String email = readLine("Email: ");
        String password = readLine("Password: ");

        try {
            LoginResponse response = authApi.login(
                email,
                password
            );

            System.out.println();
            System.out.println("Login successful!");
            System.out.println(
                "Welcome, " + response.name() + "!"
            );

        } catch (Exception e) {
            printError(e.getMessage());
        }
    }
}
