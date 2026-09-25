package dev.hostapp.cli;

import dev.hostapp.cli.api.ApiClient;
import dev.hostapp.cli.api.AuthApi;
import dev.hostapp.cli.screens.BaseScreen;
import dev.hostapp.cli.screens.auth.LoginScreen;
import dev.hostapp.cli.screens.auth.RegisterScreen;

import java.util.Scanner;

public class App {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        ApiClient apiClient = new ApiClient(
            "http://localhost:8080"
        );

        AuthApi authApi = new AuthApi(apiClient);

        BaseScreen registerScreen = new RegisterScreen(
            scanner,
            authApi
        );

        BaseScreen loginScreen = new LoginScreen(
            scanner,
            authApi
        );

        while (true) {
            System.out.println();
            System.out.println("=== Host App ===");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("0. Exit");
            System.out.print("> ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1" -> registerScreen.show();

                case "2" -> loginScreen.show();

                case "0" -> {
                    System.out.println("Bye!");
                    scanner.close();
                    return;
                }

                default -> System.out.println(
                    "Unknown command"
                );
            }
        }
    }
}
