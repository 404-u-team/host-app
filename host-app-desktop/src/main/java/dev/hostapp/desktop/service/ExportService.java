package dev.hostapp.desktop.service;

import dev.hostapp.desktop.dto.ServerRequestResponse;
import dev.hostapp.desktop.util.Display;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ExportService {

    public void exportRequests(Path file, List<ServerRequestResponse> requests) throws IOException {
        StringBuilder csv = new StringBuilder("\uFEFFОС;CPU;RAM (ГБ);Диск (ГБ);Статус;Создана;ID заявки;ID сервера\r\n");
        for (ServerRequestResponse request : requests) {
            csv.append(cell(request.os())).append(';')
                    .append(request.cpuCores()).append(';')
                    .append(request.ramGb()).append(';')
                    .append(request.diskGb()).append(';')
                    .append(cell(Display.status(request.status()))).append(';')
                    .append(cell(Display.date(request.createdAt()))).append(';')
                    .append(cell(request.id().toString())).append(';')
                    .append(cell(request.serverId() == null ? "" : request.serverId().toString()))
                    .append("\r\n");
        }
        Files.writeString(file, csv, StandardCharsets.UTF_8);
    }

    private String cell(String value) {
        String text = value;
        String trimmed = text.stripLeading();
        if (trimmed.startsWith("=") || trimmed.startsWith("+") || trimmed.startsWith("-")
                || trimmed.startsWith("@")) {
            text = "'" + text;
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
