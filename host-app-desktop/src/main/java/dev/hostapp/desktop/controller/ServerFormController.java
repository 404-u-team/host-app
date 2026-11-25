package dev.hostapp.desktop.controller;

import dev.hostapp.desktop.App;
import dev.hostapp.desktop.dto.server.ServerData;
import dev.hostapp.desktop.dto.server.ServerResponse;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ServerFormController extends BaseController {

    @FXML private TextField hostnameField;
    @FXML private TextField cpuField;
    @FXML private TextField ramField;
    @FXML private TextField diskField;
    @FXML private TextField ipv4Field;
    @FXML private TextField ipv6Field;
    private Stage window;
    private ServerResponse selected;
    private boolean saved;

    public void setup(App app, Stage window, ServerResponse selected) {
        super.setup(app);
        this.window = window;
        this.selected = selected;
        hostnameField.setText(selected.hostname());
        cpuField.setText(selected.cpuCores().toString());
        ramField.setText(selected.ramGb().toString());
        diskField.setText(selected.diskGb().toString());
        ipv4Field.setText(String.join(", ", selected.ipv4Addresses()));
        ipv6Field.setText(String.join(", ", selected.ipv6Addresses()));
    }

    @FXML
    private void save() {
        try {
            ServerData data = app.getServerService().formData(hostnameField.getText(), cpuField.getText(),
                    ramField.getText(), diskField.getText(), ipv4Field.getText(), ipv6Field.getText());
            runTask(() -> {
                app.getServerService().update(selected, data);
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
