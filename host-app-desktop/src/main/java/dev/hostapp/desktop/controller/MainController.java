package dev.hostapp.desktop.controller;

import dev.hostapp.desktop.App;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class MainController extends BaseController {

    @FXML private Label userLabel;
    @FXML private TabPane tabs;
    @FXML private Tab requestsTab;
    @FXML private Tab serversTab;
    @FXML private RequestsController requestsController;
    @FXML private ServersController serversController;
    @FXML private StatisticsController statisticsController;

    @Override
    public void setup(App app) {
        super.setup(app);
        String role = app.isAdmin() ? "Администратор" : "Пользователь";
        userLabel.setText(app.getUser().name() + " | " + role);
        requestsController.setup(app);
        serversController.setup(app);
        statisticsController.setup(app);
        tabs.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) -> {
            if (selected == requestsTab) {
                requestsController.refresh();
            } else if (selected == serversTab) {
                serversController.refresh();
            } else {
                statisticsController.refresh();
            }
        });
        requestsController.refresh();
    }

    @FXML
    private void logout() {
        app.logout();
    }
}
