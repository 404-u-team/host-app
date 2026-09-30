package dev.hostapp.cli.screens;

import dev.hostapp.cli.api.ServerRequestApi;
import dev.hostapp.cli.dto.StatisticsResponse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.UUID;

public class MainMenuScreen extends BaseScreen {

    private final ServerRequestApi requestApi;

    public MainMenuScreen(Scanner scanner, ServerRequestApi requestApi) {
        super(scanner);
        this.requestApi = requestApi;
    }

    @Override
    public void show() {
        while (true) {
            System.out.println();
            System.out.println("========================================");
            System.out.println("HOST APP");
            System.out.println("========================================");
            System.out.println("1. Заявки");
            System.out.println("2. Поиск");
            System.out.println("3. Фильтрация");
            System.out.println("4. Сортировка");
            System.out.println("5. Статистика");
            System.out.println("6. Экспорт XLSX");
            System.out.println("7. Выход");

            switch (readLine("Выберите пункт: ")) {
                case "1" -> requestsMenu();
                case "2" -> searchMenu();
                case "3" -> filterMenu();
                case "4" -> sortMenu();
                case "5" -> showStatistics();
                case "6" -> export();
                case "7" -> {
                    return;
                }
                default -> printError("неизвестный пункт меню");
            }
        }
    }

    private void requestsMenu() {
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

    private void searchMenu() {
        while (true) {
            System.out.println();
            System.out.println("=== Поиск ===");
            System.out.println("1. По UUID");
            System.out.println("2. По операционной системе");
            System.out.println("3. По минимальному CPU");
            System.out.println("0. Назад");
            switch (readLine("Выберите пункт: ")) {
                case "1" -> showById();
                case "2" -> searchByOs();
                case "3" -> searchByMinCpu();
                case "0" -> {
                    return;
                }
                default -> printError("неизвестный пункт меню");
            }
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

    private void filterMenu() {
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

    private void sortMenu() {
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

    private void showStatistics() {
        try {
            StatisticsResponse statistics = requestApi.getStatistics();
            System.out.println("Всего заявок: " + statistics.total());
            System.out.println("CREATED: " + statistics.created());
            System.out.println("APPROVED: " + statistics.approved());
            System.out.println("REJECTED: " + statistics.rejected());
            System.out.println("COMPLETED: " + statistics.completed());
            System.out.println("CANCELLED: " + statistics.cancelled());
        } catch (Exception exception) {
            printError(exception.getMessage());
        }
    }

    private void export() {
        try {
            Path file = Path.of("server-requests.xlsx").toAbsolutePath();
            Files.write(file, requestApi.export());
            System.out.println("Файл создан: " + file);
        } catch (Exception exception) {
            printError(exception.getMessage());
        }
    }
}
