package dev.hostapp.cli.screens.requests;

import dev.hostapp.cli.api.ServerRequestApi;
import dev.hostapp.cli.dto.StatisticsResponse;
import dev.hostapp.cli.screens.BaseScreen;

import java.util.Scanner;

public class StatisticsScreen extends BaseScreen {

    private final ServerRequestApi requestApi;

    public StatisticsScreen(Scanner scanner, ServerRequestApi requestApi) {
        super(scanner);
        this.requestApi = requestApi;
    }

    @Override
    public void show() {
        try {
            StatisticsResponse statistics = requestApi.getStatistics();
            System.out.println("Всего заявок: " + statistics.total());
            System.out.println("CREATED: " + statistics.created());
            System.out.println("REJECTED: " + statistics.rejected());
            System.out.println("COMPLETED: " + statistics.completed());
            System.out.println("CANCELLED: " + statistics.cancelled());
        } catch (Exception exception) {
            printError(exception.getMessage());
        }
    }
}
