package dev.hostapp.desktop.controller;

import dev.hostapp.desktop.App;
import dev.hostapp.desktop.dto.ServerRequestData;
import dev.hostapp.desktop.dto.ServerRequestResponse;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RequestFormController extends BaseController {

    @FXML private TextField cpuField;
    @FXML private TextField ramField;
    @FXML private TextField diskField;
    @FXML private TextField osField;
    private Stage window;
    private ServerRequestResponse selected;
    private boolean saved;

    public void setup(App app, Stage window, ServerRequestResponse selected) {
        super.setup(app);
        this.window = window;
        this.selected = selected;
        if (selected != null) {
            cpuField.setText(selected.cpuCores().toString());
            ramField.setText(selected.ramGb().toString());
            diskField.setText(selected.diskGb().toString());
            osField.setText(selected.os());
        }
    }

    @FXML
    private void save() {
        try {
            ServerRequestData data = app.getRequestService().formData(cpuField.getText(), ramField.getText(),
                    diskField.getText(), osField.getText());
            runTask(() -> {
                app.getRequestService().save(selected, data);
                return null;
            }, result -> {
                saved = true;
                window.close();
            });
        } catch (IllegalArgumentException exception) {
            app.handleError(exception);
        }
    }

    @FXML
    private void cancel() {
        window.close();
    }

    public boolean isSaved() {
        return saved;
    }
}
