package dev.hostapp.cli.screens.requests;

import dev.hostapp.cli.api.ServerRequestApi;
import dev.hostapp.cli.screens.BaseScreen;

import java.util.Scanner;

public class FilterScreen extends BaseScreen {

    private final ServerRequestApi requestApi;

    public FilterScreen(Scanner scanner, ServerRequestApi requestApi) {
        super(scanner);
        this.requestApi = requestApi;
    }

    @Override
    public void show() {
        while (true) {
            System.out.println();
            System.out.println("=== Фильтрация ===");
            System.out.println("1. По статусу");
            System.out.println("2. По операционной системе");
            System.out.println("0. Назад");

            switch (readLine("Выберите пункт: ")) {
                case "1" -> filterByStatus();
                case "2" -> filterByOs();
                case "0" -> {
                    return;
                }
                default -> printError("неизвестный пункт меню");
            }
        }
    }

    private void filterByStatus() {
        String status = readChoice("Статус (CREATED, APPROVED, REJECTED, COMPLETED, CANCELLED): ",
                "CREATED", "APPROVED", "REJECTED", "COMPLETED", "CANCELLED");
        try {
            printRequests(requestApi.filterByStatus(status));
        } catch (Exception exception) {
            printError(exception.getMessage());
        }
    }

    private void filterByOs() {
        String os = readNonEmpty("OS: ");
        try {
            printRequests(requestApi.filterByOs(os));
        } catch (Exception exception) {
            printError(exception.getMessage());
        }
    }
}
