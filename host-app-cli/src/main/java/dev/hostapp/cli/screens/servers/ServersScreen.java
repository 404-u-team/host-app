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
                System.out.println("3. Изменить сервер");
                System.out.println("4. Удалить сервер");
            }
            System.out.println("0. Назад");

            switch (readLine("Выберите пункт: ")) {
                case "1" -> showAll();
                case "2" -> showById();
                case "3" -> {
                    if (admin) update();
                    else printError("неизвестный пункт меню");
                }
                case "4" -> {
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

    private void delete() {
        UUID id = readUuid("ID сервера: ");
        String confirmation = readChoice("Удалить сервер и все связанные заявки? (ДА/НЕТ): ", "ДА", "НЕТ");
        if (confirmation.equals("НЕТ")) {
            return;
        }
        try {
            serverApi.delete(id);
            System.out.println("Сервер и связанные заявки удалены");
        } catch (Exception exception) {
            printError(exception.getMessage());
        }
    }
}
