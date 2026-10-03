package dev.hostapp.cli;

import dev.hostapp.cli.api.ApiClient;
import dev.hostapp.cli.api.AuthApi;
import dev.hostapp.cli.api.ServerRequestApi;
import dev.hostapp.cli.api.ServerApi;
import dev.hostapp.cli.screens.BaseScreen;
import dev.hostapp.cli.screens.MainMenuScreen;
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
        ServerRequestApi requestApi = new ServerRequestApi(apiClient);
        ServerApi serverApi = new ServerApi(apiClient);

        BaseScreen registerScreen = new RegisterScreen(
            scanner,
            authApi
        );

        LoginScreen loginScreen = new LoginScreen(
            scanner,
            authApi
        );

        while (true) {
            System.out.println();
            System.out.println("=== HOST APP ===");
            System.out.println("1. Регистрация");
            System.out.println("2. Вход");
            System.out.println("0. Выход");
            System.out.print("Выберите пункт: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1" -> registerScreen.show();

                case "2" -> {
                    if (loginScreen.login()) {
                        new MainMenuScreen(scanner, requestApi, serverApi, loginScreen.isAdmin()).show();
                        System.out.println("До свидания!");
                        scanner.close();
                        return;
                    }
                }

                case "0" -> {
                    System.out.println("До свидания!");
                    scanner.close();
                    return;
                }

                default -> System.out.println("Ошибка: неизвестный пункт меню");
            }
        }
    }
}
