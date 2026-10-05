package dev.hostapp.cli.screens;

import dev.hostapp.cli.api.ServerRequestApi;
import dev.hostapp.cli.api.ServerApi;
import dev.hostapp.cli.screens.requests.ExportScreen;
import dev.hostapp.cli.screens.requests.FilterScreen;
import dev.hostapp.cli.screens.requests.RequestsScreen;
import dev.hostapp.cli.screens.requests.SearchScreen;
import dev.hostapp.cli.screens.requests.SortScreen;
import dev.hostapp.cli.screens.requests.StatisticsScreen;
import dev.hostapp.cli.screens.servers.ServersScreen;

import java.util.Scanner;

public class MainMenuScreen extends BaseScreen {

    private final ServerRequestApi requestApi;
    private final ServerApi serverApi;
    private final boolean admin;

    public MainMenuScreen(Scanner scanner, ServerRequestApi requestApi, ServerApi serverApi, boolean admin) {
        super(scanner);
        this.requestApi = requestApi;
        this.serverApi = serverApi;
        this.admin = admin;
    }

    @Override
    public void show() {
        while (true) {
            System.out.println();
            System.out.println("========================================");
            System.out.println("HOST APP");
            System.out.println("========================================");
            System.out.println("1. Заявки");
            System.out.println("2. Серверы");
            System.out.println("3. Поиск заявок");
            System.out.println("4. Фильтрация заявок");
            System.out.println("5. Сортировка заявок");
            System.out.println("6. Статистика заявок");
            System.out.println("7. Экспорт XLSX");
            System.out.println("0. Выход");

            switch (readLine("Выберите пункт: ")) {
                case "1" -> new RequestsScreen(scanner, requestApi, admin).show();
                case "2" -> new ServersScreen(scanner, serverApi, admin).show();
                case "3" -> new SearchScreen(scanner, requestApi).show();
                case "4" -> new FilterScreen(scanner, requestApi).show();
                case "5" -> new SortScreen(scanner, requestApi).show();
                case "6" -> new StatisticsScreen(scanner, requestApi).show();
                case "7" -> new ExportScreen(scanner, requestApi).show();
                case "0" -> {
                    return;
                }
                default -> printError("неизвестный пункт меню");
            }
        }
    }
}
