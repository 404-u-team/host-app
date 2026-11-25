package dev.hostapp.desktop.util;

import dev.hostapp.desktop.App;
import dev.hostapp.desktop.dto.server.ServerResponse;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class Dialogs {

    public static Stage form(Window owner, String title, Parent content) {
        Stage window = new Stage();
        window.initOwner(owner);
        window.initModality(Modality.WINDOW_MODAL);
        window.setTitle(title);
        Scene scene = new Scene(content);
        scene.getStylesheets().add(App.class.getResource("/dev/hostapp/desktop/style.css").toExternalForm());
        window.setScene(scene);
        window.setMinWidth(480);
        window.setMinHeight(360);
        window.setOnCloseRequest(event -> {
            if (content.isDisabled()) {
                event.consume();
            }
        });
        return window;
    }

    public static void error(Window owner, String message) {
        show(owner, Alert.AlertType.ERROR, "Ошибка", message);
    }

    public static void info(Window owner, String title, String message) {
        show(owner, Alert.AlertType.INFORMATION, title, message);
    }

    public static boolean confirm(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.initOwner(owner);
        alert.setTitle("Подтверждение");
        alert.setHeaderText(null);
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    public static void server(Window owner, ServerResponse server) {
        info(owner, "Сервер", "Имя: " + server.hostname() + "\nОС: " + server.os()
                + "\nCPU: " + server.cpuCores() + "\nRAM: " + server.ramGb() + " ГБ"
                + "\nДиск: " + server.diskGb() + " ГБ"
                + "\nIPv4: " + String.join(", ", server.ipv4Addresses())
                + "\nIPv6: " + String.join(", ", server.ipv6Addresses())
                + "\nСоздан: " + Display.date(server.createdAt()) + "\nID: " + server.id());
    }

    private static void show(Window owner, Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        Window activeOwner = owner;
        for (Window window : Window.getWindows()) {
            if (window instanceof Stage stage && stage.getOwner() == owner && window.isShowing()) {
                activeOwner = window;
            }
        }
        alert.initOwner(activeOwner);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.setResizable(true);
        alert.showAndWait();
    }
}
