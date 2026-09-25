package dev.hostapp.cli.screens;

import java.util.Scanner;

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
        System.out.println("Error: " + message);
    }
}