package dev.hostapp.cli.screens.requests;

import dev.hostapp.cli.api.ServerRequestApi;
import dev.hostapp.cli.screens.BaseScreen;

import java.util.Scanner;
import java.util.UUID;

public class RequestsScreen extends BaseScreen {

    private final ServerRequestApi requestApi;

    public RequestsScreen(Scanner scanner, ServerRequestApi requestApi) {
        super(scanner);
        this.requestApi = requestApi;
    }

    @Override
    public void show() {
        while (true) {
            System.out.println();
            System.out.println("=== Заявки ===");
            System.out.println("1. Показать все");
            System.out.println("2. Получить по ID");
            System.out.println("3. Создать");
            System.out.println("4. Изменить");
            System.out.println("5. Удалить");
            System.out.println("6. Назад");

            switch (readLine("Выберите пункт: ")) {
                case "1" -> showAll();
                case "2" -> showById();
                case "3" -> create();
                case "4" -> update();
                case "5" -> delete();
                case "6" -> {
                    return;
                }
                default -> printError("неизвестный пункт меню");
            }
        }
    }

    private void showAll() {
        try {
            printRequests(requestApi.getAll());
        } catch (Exception exception) {
            printError(exception.getMessage());
        }
    }

    private void showById() {
        UUID id = readUuid("ID заявки: ");
        try {
            printRequest(requestApi.get(id));
        } catch (Exception exception) {
            printError(exception.getMessage());
        }
    }

    private void create() {
        int cpu = readPositiveInteger("CPU: ");
        int ram = readPositiveInteger("RAM (GB): ");
        int disk = readPositiveInteger("Disk (GB): ");
        String os = readNonEmpty("OS: ");
        try {
            UUID id = requestApi.create(cpu, ram, disk, os);
            System.out.println("Заявка создана: " + id);
        } catch (Exception exception) {
            printError(exception.getMessage());
        }
    }

    private void update() {
        UUID id = readUuid("ID заявки: ");
        int cpu = readPositiveInteger("CPU: ");
        int ram = readPositiveInteger("RAM (GB): ");
        int disk = readPositiveInteger("Disk (GB): ");
        String os = readNonEmpty("OS: ");
        try {
            printRequest(requestApi.update(id, cpu, ram, disk, os));
        } catch (Exception exception) {
            printError(exception.getMessage());
        }
    }

    private void delete() {
        UUID id = readUuid("ID заявки: ");
        try {
            requestApi.delete(id);
            System.out.println("Заявка удалена");
        } catch (Exception exception) {
            printError(exception.getMessage());
        }
    }
}
