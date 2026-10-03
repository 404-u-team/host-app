package dev.hostapp.cli.screens.requests;

import dev.hostapp.cli.api.ServerRequestApi;
import dev.hostapp.cli.screens.BaseScreen;

import java.util.Scanner;

public class SortScreen extends BaseScreen {

    private final ServerRequestApi requestApi;

    public SortScreen(Scanner scanner, ServerRequestApi requestApi) {
        super(scanner);
        this.requestApi = requestApi;
    }

    @Override
    public void show() {
        while (true) {
            System.out.println();
            System.out.println("=== Сортировка ===");
            System.out.println("1. По дате");
            System.out.println("2. По CPU");
            System.out.println("3. По RAM");
            System.out.println("0. Назад");

            switch (readLine("Выберите пункт: ")) {
                case "1" -> sortBy("date");
                case "2" -> sortBy("cpu");
                case "3" -> sortBy("ram");
                case "0" -> {
                    return;
                }
                default -> printError("неизвестный пункт меню");
            }
        }
    }

    private void sortBy(String field) {
        try {
            printRequests(requestApi.sortBy(field));
        } catch (Exception exception) {
            printError(exception.getMessage());
        }
    }
}
