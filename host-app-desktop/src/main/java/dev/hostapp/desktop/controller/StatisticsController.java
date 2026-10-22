package dev.hostapp.desktop.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class StatisticsController extends BaseController {

    @FXML private Label totalLabel;
    @FXML private Label createdLabel;
    @FXML private Label approvedLabel;
    @FXML private Label completedLabel;
    @FXML private Label rejectedLabel;
    @FXML private Label cancelledLabel;

    @FXML
    public void refresh() {
        runTask(() -> app.getRequestService().statistics(), statistics -> {
            totalLabel.setText(Long.toString(statistics.total()));
            createdLabel.setText(Long.toString(statistics.created()));
            approvedLabel.setText(Long.toString(statistics.approved()));
            completedLabel.setText(Long.toString(statistics.completed()));
            rejectedLabel.setText(Long.toString(statistics.rejected()));
            cancelledLabel.setText(Long.toString(statistics.cancelled()));
        });
    }
}
