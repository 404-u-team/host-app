package dev.hostapp.cli.screens.servers;

import dev.hostapp.cli.api.ServerApi;
import dev.hostapp.cli.dto.server.ServerData;
import dev.hostapp.cli.dto.server.ServerResponse;
import dev.hostapp.cli.screens.BaseScreen;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class ServersScreen extends BaseScreen {

    private final ServerApi serverApi;
    private final boolean admin;

    public ServersScreen(Scanner scanner, ServerApi serverApi, boolean admin) {
        super(scanner);
        this.serverApi = serverApi;
        this.admin = admin;
    }

    @Override
    public void show() {
        while (true) {
            System.out.println();
            System.out.println(admin ? "=== Управление серверами ===" : "=== Мои серверы ===");
            System.out.println("1. Показать серверы");
            System.out.println("2. Получить сервер по ID");
            if (admin) {
                System.out.println("3. Создать сервер");
                System.out.println("4. Изменить сервер");
                System.out.println("5. Запустить сервер");
                System.out.println("6. Остановить сервер");
                System.out.println("7. Изменить статус");
                System.out.println("8. Удалить сервер");
            }
            System.out.println("0. Назад");

            switch (readLine("Выберите пункт: ")) {
                case "1" -> showAll();
                case "2" -> showById();
                case "3" -> {
                    if (admin) create();
                    else printError("неизвестный пункт меню");
                }
                case "4" -> {
                    if (admin) update();
                    else printError("неизвестный пункт меню");
                }
                case "5" -> {
                    if (admin) updateStatus("ON");
                    else printError("неизвестный пункт меню");
                }
                case "6" -> {
                    if (admin) updateStatus("OFF");
                    else printError("неизвестный пункт меню");
                }
                case "7" -> {
                    if (admin) chooseStatus();
                    else printError("неизвестный пункт меню");
                }
                case "8" -> {
                    if (admin) delete();
                    else printError("неизвестный пункт меню");
                }
                case "0" -> {
                    return;
                }
                default -> printError("неизвестный пункт меню");
            }
        }
    }

    private void showAll() {
        try {
            printServers(serverApi.getAll());
        } catch (Exception exception) {
            printError(exception.getMessage());
        }
    }

    private void showById() {
        UUID id = readUuid("ID сервера: ");
        try {
            printServer(serverApi.get(id));
        } catch (Exception exception) {
            printError(exception.getMessage());
        }
    }

    private void create() {
        ServerData server = readServerData();
        try {
            ServerResponse created = serverApi.create(server);
            System.out.println("Сервер создан");
            printServer(created);
        } catch (Exception exception) {
            printError(exception.getMessage());
        }
    }

    private void update() {
        UUID id = readUuid("ID сервера: ");
        ServerData server = readServerData();
        try {
            printServer(serverApi.update(id, server));
        } catch (Exception exception) {
            printError(exception.getMessage());
        }
    }

    private ServerData readServerData() {
        String hostname = readNonEmpty("Имя сервера: ");
        List<String> ipv4Addresses = readAddresses("IPv4-адреса через запятую (можно пусто): ");
        List<String> ipv6Addresses = readAddresses("IPv6-адреса через запятую (можно пусто): ");
        int cpu = readPositiveInteger("CPU cores: ");
        int ram = readPositiveInteger("RAM (GB): ");
        int disk = readPositiveInteger("Disk (GB): ");
        return new ServerData(hostname, ipv4Addresses, ipv6Addresses, cpu, ram, disk);
    }

    private List<String> readAddresses(String label) {
        String value = readLine(label).trim();
        if (value.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(address -> !address.isEmpty())
                .distinct()
                .toList();
    }

    private void updateStatus(String status) {
        UUID id = readUuid("ID сервера: ");
        try {
            printServer(serverApi.updateStatus(id, status));
        } catch (Exception exception) {
            printError(exception.getMessage());
        }
    }

    private void chooseStatus() {
        UUID id = readUuid("ID сервера: ");
        String status = readChoice("Статус (ON, OFF, SUSPENDED): ", "ON", "OFF", "SUSPENDED");
        try {
            printServer(serverApi.updateStatus(id, status));
        } catch (Exception exception) {
            printError(exception.getMessage());
        }
    }

    private void delete() {
        UUID id = readUuid("ID сервера: ");
        String confirmation = readChoice("Удалить сервер? (ДА/НЕТ): ", "ДА", "НЕТ");
        if (confirmation.equals("НЕТ")) {
            return;
        }
        try {
            serverApi.delete(id);
            System.out.println("Сервер удалён");
        } catch (Exception exception) {
            printError(exception.getMessage());
        }
    }
}
