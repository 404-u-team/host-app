package dev.hostapp.desktop.controller;

import dev.hostapp.desktop.App;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Parent;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public abstract class BaseController {

    @FXML protected Parent root;
    protected App app;

    public void setup(App app) {
        this.app = app;
    }

    protected <T> void runTask(Callable<T> operation, Consumer<T> onSuccess) {
        Parent mainRoot = app.getStage().getScene().getRoot();
        root.setDisable(true);
        mainRoot.setDisable(true);
        app.getStage().getScene().setCursor(javafx.scene.Cursor.WAIT);
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return operation.call();
            }
        };
        task.setOnSucceeded(event -> {
            finishTask(mainRoot);
            try {
                onSuccess.accept(task.getValue());
            } catch (RuntimeException exception) {
                app.handleError(exception);
            }
        });
        task.setOnFailed(event -> {
            finishTask(mainRoot);
            app.handleError(task.getException());
        });
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void finishTask(Parent mainRoot) {
        root.setDisable(false);
        mainRoot.setDisable(false);
        app.getStage().getScene().setCursor(javafx.scene.Cursor.DEFAULT);
    }
}
