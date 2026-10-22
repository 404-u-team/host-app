package dev.hostapp.desktop.controller;

import dev.hostapp.desktop.App;
import dev.hostapp.desktop.dto.server.ServerResponse;
import dev.hostapp.desktop.util.Dialogs;
import dev.hostapp.desktop.util.TableColumns;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.io.IOException;

public class ServersController extends BaseController {

    @FXML private TableView<ServerResponse> table;
    @FXML private TableColumn<ServerResponse, String> hostnameColumn;
    @FXML private TableColumn<ServerResponse, String> osColumn;
    @FXML private TableColumn<ServerResponse, Integer> cpuColumn;
    @FXML private TableColumn<ServerResponse, Integer> ramColumn;
    @FXML private TableColumn<ServerResponse, Integer> diskColumn;
    @FXML private TableColumn<ServerResponse, String> ipv4Column;
    @FXML private TableColumn<ServerResponse, String> dateColumn;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button detailsButton;
    @FXML private Label countLabel;

    @FXML
    private void initialize() {
        hostnameColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().hostname()));
        osColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().os()));
        cpuColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().cpuCores()));
        ramColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().ramGb()));
        diskColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().diskGb()));
        ipv4Column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(String.join(", ", cell.getValue().ipv4Addresses())));
        dateColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().createdAt()));
        TableColumns.date(dateColumn);
        table.setItems(FXCollections.observableArrayList());
        table.setPlaceholder(new Label("Серверов нет. Сервер появляется после выполнения заявки администратором."));
        table.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) -> updateButtons());
    }

    @Override
    public void setup(App app) {
        super.setup(app);
        editButton.setVisible(app.isAdmin());
        editButton.setManaged(app.isAdmin());
        deleteButton.setVisible(app.isAdmin());
        deleteButton.setManaged(app.isAdmin());
        updateButtons();
    }

    @FXML
    public void refresh() {
        runTask(() -> app.getServerService().getAll(), servers -> {
            table.getItems().setAll(servers);
            table.sort();
            countLabel.setText("Серверов: " + servers.size());
            updateButtons();
        });
    }

    @FXML
    private void details() {
        ServerResponse selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Dialogs.server(app.getStage(), selected);
        }
    }

    @FXML
    private void edit() {
        ServerResponse selected = table.getSelectionModel().getSelectedItem();
        if (selected == null || !app.isAdmin()) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/dev/hostapp/desktop/server-form.fxml"));
            Parent content = loader.load();
            Stage window = Dialogs.form(app.getStage(), "Редактирование сервера", content);
            ServerFormController controller = loader.getController();
            controller.setup(app, window, selected);
            window.showAndWait();
            if (controller.isSaved()) {
                refresh();
            }
        } catch (IOException exception) {
            app.handleError(exception);
        }
    }

    @FXML
    private void delete() {
        ServerResponse selected = table.getSelectionModel().getSelectedItem();
        if (selected != null && app.isAdmin() && Dialogs.confirm(app.getStage(), "Удалить сервер «"
                + selected.hostname() + "»? Все связанные заявки тоже будут удалены.")) {
            runTask(() -> {
                app.getServerService().delete(selected);
                return null;
            }, result -> refresh());
        }
    }

    private void updateButtons() {
        boolean noSelection = table.getSelectionModel().getSelectedItem() == null;
        editButton.setDisable(noSelection);
        deleteButton.setDisable(noSelection);
        detailsButton.setDisable(noSelection);
    }
}
