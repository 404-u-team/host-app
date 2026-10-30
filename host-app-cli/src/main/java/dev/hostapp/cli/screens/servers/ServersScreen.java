package dev.hostapp.cli.screens.servers;

import dev.hostapp.cli.api.ServerApi;
import dev.hostapp.cli.api.ServerRequestApi;
import dev.hostapp.cli.dto.ServerRequestData;
import dev.hostapp.cli.dto.server.ServerData;
import dev.hostapp.cli.dto.server.ServerResponse;
import dev.hostapp.cli.screens.BaseScreen;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class ServersScreen extends BaseScreen {

    private final ServerApi serverApi;
    private final ServerRequestApi requestApi;
    private final boolean admin;

    public ServersScreen(Scanner scanner, ServerApi serverApi, ServerRequestApi requestApi, boolean admin) {
        super(scanner);
        this.serverApi = serverApi;
        this.requestApi = requestApi;
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
            } else {
                System.out.println("3. Запросить изменение сервера");
                System.out.println("4. Запросить отмену сервера");
            }
            System.out.println("0. Назад");

            switch (readLine("Выберите пункт: ")) {
                case "1" -> showAll();
                case "2" -> showById();
                case "3" -> {
                    if (admin) update();
                    else requestChange();
                }
                case "4" -> {
                    if (admin) delete();
                    else requestCancellation();
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

    private void requestChange() {
        UUID id = readUuid("ID сервера: ");
        int cpu = readPositiveInteger("CPU: ");
        int ram = readPositiveInteger("RAM (GB): ");
        int disk = readPositiveInteger("Disk (GB): ");
        String os = readNonEmpty("OS: ");
        try {
            UUID requestId = requestApi.requestChange(id, new ServerRequestData(cpu, ram, disk, os));
            System.out.println("Заявка на изменение создана: " + requestId);
        } catch (Exception exception) {
            printError(exception.getMessage());
        }
    }

    private void requestCancellation() {
        UUID id = readUuid("ID сервера: ");
        String confirmation = readChoice("Запросить отмену? После одобрения сервер и все связанные заявки удалятся. (ДА/НЕТ): ", "ДА", "НЕТ");
        if (confirmation.equals("НЕТ")) {
            return;
        }
        try {
            System.out.println("Заявка на отмену создана: " + requestApi.requestCancellation(id));
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
