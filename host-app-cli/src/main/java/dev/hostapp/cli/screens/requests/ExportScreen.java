package dev.hostapp.cli.screens.requests;

import dev.hostapp.cli.api.ServerRequestApi;
import dev.hostapp.cli.screens.BaseScreen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class ExportScreen extends BaseScreen {

    private final ServerRequestApi requestApi;

    public ExportScreen(Scanner scanner, ServerRequestApi requestApi) {
        super(scanner);
        this.requestApi = requestApi;
    }

    @Override
    public void show() {
        try {
            Path file = Path.of("server-requests.xlsx").toAbsolutePath();
            Files.write(file, requestApi.export());
            System.out.println("Файл создан: " + file);
        } catch (Exception exception) {
            printError(exception.getMessage());
        }
    }
}
