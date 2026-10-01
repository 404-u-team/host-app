package dev.hostapp.cli.screens;

import dev.hostapp.cli.dto.ServerRequestResponse;

import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public abstract class BaseScreen {

    protected final Scanner scanner;

    protected BaseScreen(Scanner scanner) {
        this.scanner = scanner;
    }

    public abstract void show();

    protected String readLine(String label) {
        System.out.print(label);
        return scanner.nextLine();
    }

    protected void printError(String message) {
        System.out.println("Ошибка: " + message);
    }

    protected int readPositiveInteger(String label) {
        while (true) {
            String value = readLine(label);
            try {
                int number = Integer.parseInt(value);
                if (number <= 0) {
                    printError("значение должно быть больше 0");
                    continue;
                }
                return number;
            } catch (NumberFormatException exception) {
                printError("нужно ввести число");
            }
        }
    }

    protected UUID readUuid(String label) {
        while (true) {
            String value = readLine(label);
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException exception) {
                printError("неверный UUID");
            }
        }
    }

    protected String readChoice(String label, String... choices) {
        while (true) {
            String value = readLine(label).trim().toUpperCase();
            for (String choice : choices) {
                if (choice.equals(value)) {
                    return value;
                }
            }
            printError("допустимые значения: " + String.join(", ", choices));
        }
    }

    protected String readNonEmpty(String label) {
        while (true) {
            String value = readLine(label).trim();
            if (!value.isEmpty()) {
                return value;
            }
            printError("поле не должно быть пустым");
        }
    }

    protected void printRequest(ServerRequestResponse request) {
        System.out.println("ID: " + request.id());
        System.out.println("CPU: " + request.cpuCores() + " | RAM: " + request.ramGb()
                + " GB | Disk: " + request.diskGb() + " GB");
        System.out.println("OS: " + request.os() + " | Статус: " + request.status());
        System.out.println("Создана: " + request.createdAt());
    }

    protected void printRequests(List<ServerRequestResponse> requests) {
        if (requests.isEmpty()) {
            System.out.println("Заявок нет");
            return;
        }
        for (ServerRequestResponse request : requests) {
            System.out.println();
            printRequest(request);
        }
    }
}
