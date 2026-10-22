package dev.hostapp.desktop.util;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

import java.time.Instant;
import java.util.Comparator;

public class TableColumns {

    public static <T> void date(TableColumn<T, String> column) {
        column.setComparator(Comparator.comparing(Instant::parse));
        column.setCellFactory(value -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : Display.date(item));
            }
        });
    }
}
