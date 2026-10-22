package dev.hostapp.desktop.controller;

import dev.hostapp.desktop.App;
import dev.hostapp.desktop.dto.ServerRequestResponse;
import dev.hostapp.desktop.service.ExportService;
import dev.hostapp.desktop.util.Dialogs;
import dev.hostapp.desktop.util.Display;
import dev.hostapp.desktop.util.TableColumns;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RequestsController extends BaseController {

    @FXML private TableView<ServerRequestResponse> table;
    @FXML private TableColumn<ServerRequestResponse, String> osColumn;
    @FXML private TableColumn<ServerRequestResponse, Integer> cpuColumn;
    @FXML private TableColumn<ServerRequestResponse, Integer> ramColumn;
    @FXML private TableColumn<ServerRequestResponse, Integer> diskColumn;
    @FXML private TableColumn<ServerRequestResponse, String> statusColumn;
    @FXML private TableColumn<ServerRequestResponse, String> dateColumn;
    @FXML private TextField searchField;
    @FXML private TextField minCpuField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> osFilter;
    @FXML private Label countLabel;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button detailsButton;
    @FXML private Button serverButton;
    @FXML private Button statusButton;

    private List<ServerRequestResponse> allRequests = new ArrayList<>();
    private final ObservableList<ServerRequestResponse> rows = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        osColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().os()));
        cpuColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().cpuCores()));
        ramColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().ramGb()));
        diskColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().diskGb()));
        statusColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(Display.status(cell.getValue().status())));
        dateColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().createdAt()));
        TableColumns.date(dateColumn);
        dateColumn.setSortType(TableColumn.SortType.DESCENDING);
        table.setItems(rows);
        table.getSortOrder().add(dateColumn);
        table.setPlaceholder(new Label("Заявок нет"));
        statusFilter.getItems().setAll("Все", "CREATED", "APPROVED", "COMPLETED", "REJECTED", "CANCELLED");
        statusFilter.setValue("Все");
        osFilter.getItems().add("Все");
        osFilter.setValue("Все");
        table.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) -> updateButtons());
    }

    @Override
    public void setup(App app) {
        super.setup(app);
        statusButton.setVisible(app.isAdmin());
        statusButton.setManaged(app.isAdmin());
        updateButtons();
    }

    @FXML
    public void refresh() {
        boolean admin = app.isAdmin();
        runTask(() -> app.getRequestService().getAll(admin), requests -> {
            allRequests = requests;
            String selectedOs = osFilter.getValue();
            osFilter.getItems().setAll("Все");
            for (ServerRequestResponse request : requests) {
                if (!osFilter.getItems().contains(request.os())) {
                    osFilter.getItems().add(request.os());
                }
            }
            osFilter.setValue(osFilter.getItems().contains(selectedOs) ? selectedOs : "Все");
            applyFilters();
        });
    }

    @FXML
    private void applyFilters() {
        try {
            List<ServerRequestResponse> filtered = app.getRequestService().filter(allRequests,
                    searchField.getText(), statusFilter.getValue(), osFilter.getValue(), minCpuField.getText());
            rows.setAll(filtered);
            table.sort();
            countLabel.setText("Показано: " + rows.size() + " из " + allRequests.size());
            updateButtons();
        } catch (IllegalArgumentException exception) {
            app.handleError(exception);
        }
    }

    @FXML
    private void resetFilters() {
        searchField.clear();
        minCpuField.clear();
        statusFilter.setValue("Все");
        osFilter.setValue("Все");
        applyFilters();
    }

    @FXML
    private void create() {
        openForm(null);
    }

    @FXML
    private void edit() {
        ServerRequestResponse selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openForm(selected);
        }
    }

    private void openForm(ServerRequestResponse selected) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/dev/hostapp/desktop/request-form.fxml"));
            Parent content = loader.load();
            Stage window = Dialogs.form(app.getStage(), selected == null ? "Новая заявка" : "Редактирование заявки", content);
            RequestFormController controller = loader.getController();
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
        ServerRequestResponse selected = table.getSelectionModel().getSelectedItem();
        if (selected != null && Dialogs.confirm(app.getStage(), "Удалить выбранную заявку на " + selected.os() + "?")) {
            runTask(() -> {
                app.getRequestService().delete(selected);
                return null;
            }, result -> refresh());
        }
    }

    @FXML
    private void details() {
        ServerRequestResponse selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Dialogs.info(app.getStage(), "Заявка", "ОС: " + selected.os()
                    + "\nCPU: " + selected.cpuCores() + "\nRAM: " + selected.ramGb() + " ГБ"
                    + "\nДиск: " + selected.diskGb() + " ГБ\nСтатус: " + Display.status(selected.status())
                    + "\nСоздана: " + Display.date(selected.createdAt())
                    + "\nОбновлена: " + Display.date(selected.updatedAt())
                    + "\nID: " + selected.id() + "\nВладелец: " + selected.ownerId()
                    + "\nСвязанный сервер: " + (selected.serverId() == null ? "ещё не создан" : selected.serverId()));
        }
    }

    @FXML
    private void showServer() {
        ServerRequestResponse selected = table.getSelectionModel().getSelectedItem();
        if (selected != null && selected.serverId() != null) {
            runTask(() -> app.getServerService().get(selected.serverId()), server -> Dialogs.server(app.getStage(), server));
        }
    }

    @FXML
    private void changeStatus() {
        ServerRequestResponse selected = table.getSelectionModel().getSelectedItem();
        if (selected == null || !app.isAdmin()) {
            return;
        }
        List<String> statuses = app.getRequestService().allowedStatuses(selected.status());
        if (statuses.isEmpty()) {
            return;
        }
        ChoiceDialog<String> dialog = new ChoiceDialog<>(statuses.get(0), statuses);
        dialog.initOwner(app.getStage());
        dialog.setTitle("Статус заявки");
        dialog.setHeaderText("COMPLETED — выполнить, REJECTED — отклонить, CANCELLED — отменить");
        dialog.setContentText("Новый статус:");
        dialog.showAndWait().ifPresent(status -> {
            String message = "Изменить статус на «" + Display.status(status) + "»?";
            if (status.equals("COMPLETED")) {
                message += " Будет создан сервер с параметрами заявки.";
            }
            if (Dialogs.confirm(app.getStage(), message)) {
                runTask(() -> {
                    app.getRequestService().changeStatus(selected, status);
                    return null;
                }, result -> refresh());
            }
        });
    }

    @FXML
    private void export() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Экспорт текущей таблицы заявок");
        chooser.setInitialFileName("requests.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File file = chooser.showSaveDialog(app.getStage());
        if (file != null) {
            List<ServerRequestResponse> currentRows = new ArrayList<>(table.getItems());
            runTask(() -> {
                new ExportService().exportRequests(file.toPath(), currentRows);
                return null;
            }, result -> Dialogs.info(app.getStage(), "Экспорт", "Сохранено заявок: " + currentRows.size()));
        }
    }

    private void updateButtons() {
        ServerRequestResponse selected = table.getSelectionModel().getSelectedItem();
        editButton.setDisable(selected == null || !selected.status().equals("CREATED"));
        deleteButton.setDisable(selected == null || selected.status().equals("COMPLETED"));
        detailsButton.setDisable(selected == null);
        serverButton.setDisable(selected == null || selected.serverId() == null);
        statusButton.setDisable(selected == null || app == null || !app.isAdmin()
                || app.getRequestService().allowedStatuses(selected.status()).isEmpty());
    }
}
