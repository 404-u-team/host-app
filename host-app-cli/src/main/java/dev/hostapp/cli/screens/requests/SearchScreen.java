package dev.hostapp.cli.screens.requests;

import dev.hostapp.cli.api.ServerRequestApi;
import dev.hostapp.cli.screens.BaseScreen;

import java.util.Scanner;
import java.util.UUID;

public class SearchScreen extends BaseScreen {

    private final ServerRequestApi requestApi;

    public SearchScreen(Scanner scanner, ServerRequestApi requestApi) {
        super(scanner);
        this.requestApi = requestApi;
    }

    @Override
    public void show() {
        while (true) {
            System.out.println();
            System.out.println("=== Поиск ===");
            System.out.println("1. По UUID");
            System.out.println("2. По операционной системе");
            System.out.println("3. По минимальному CPU");
            System.out.println("0. Назад");

            switch (readLine("Выберите пункт: ")) {
                case "1" -> searchById();
                case "2" -> searchByOs();
                case "3" -> searchByMinCpu();
                case "0" -> {
                    return;
                }
                default -> printError("неизвестный пункт меню");
            }
        }
    }

    private void searchById() {
        UUID id = readUuid("ID заявки: ");
        try {
            printRequest(requestApi.get(id));
        } catch (Exception exception) {
            printError(exception.getMessage());
        }
    }

    private void searchByOs() {
        String os = readNonEmpty("Часть названия OS: ");
        try {
            printRequests(requestApi.searchByOs(os));
        } catch (Exception exception) {
            printError(exception.getMessage());
        }
    }

    private void searchByMinCpu() {
        int minCpu = readPositiveInteger("Минимум CPU: ");
        try {
            printRequests(requestApi.searchByMinCpu(minCpu));
        } catch (Exception exception) {
            printError(exception.getMessage());
        }
    }
}
