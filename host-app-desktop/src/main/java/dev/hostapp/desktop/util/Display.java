package dev.hostapp.desktop.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Display {

    public static String status(String status) {
        return switch (status) {
            case "CREATED" -> "Создана";
            case "APPROVED" -> "Одобрена";
            case "COMPLETED" -> "Выполнена";
            case "REJECTED" -> "Отклонена";
            case "CANCELLED" -> "Отменена";
            default -> status;
        };
    }

    public static String date(String value) {
        return DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                .withZone(ZoneId.systemDefault()).format(Instant.parse(value));
    }
}
