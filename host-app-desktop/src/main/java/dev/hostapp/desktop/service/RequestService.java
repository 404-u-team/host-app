package dev.hostapp.desktop.service;

import dev.hostapp.desktop.api.ServerRequestApi;
import dev.hostapp.desktop.dto.ServerRequestData;
import dev.hostapp.desktop.dto.ServerRequestResponse;
import dev.hostapp.desktop.dto.StatisticsResponse;
import dev.hostapp.desktop.util.Display;
import dev.hostapp.desktop.util.Validation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RequestService {

    private final ServerRequestApi api;

    public RequestService(ServerRequestApi api) {
        this.api = api;
    }

    public List<ServerRequestResponse> getAll(boolean admin) throws IOException, InterruptedException {
        if (admin) {
            return api.getAllAdmin();
        }
        return api.getAll();
    }

    public ServerRequestData formData(String cpu, String ram, String disk, String os) {
        return new ServerRequestData(Validation.positiveInteger(cpu, "CPU"),
                Validation.positiveInteger(ram, "RAM"), Validation.positiveInteger(disk, "Диск"),
                Validation.text(os, "ОС", 255));
    }

    public void save(ServerRequestResponse selected, ServerRequestData data) throws IOException, InterruptedException {
        if (selected == null) {
            api.create(data.cpuCores(), data.ramGb(), data.diskGb(), data.os());
        } else {
            if (!selected.status().equals("CREATED")) {
                throw new IllegalArgumentException("Редактировать можно только созданную заявку.");
            }
            api.update(selected.id(), data.cpuCores(), data.ramGb(), data.diskGb(), data.os());
        }
    }

    public void delete(ServerRequestResponse request) throws IOException, InterruptedException {
        if (request.status().equals("COMPLETED")) {
            throw new IllegalArgumentException("Выполненную заявку удалить нельзя.");
        }
        api.delete(request.id());
    }

    public List<String> allowedStatuses(String current) {
        List<String> result = new ArrayList<>();
        if (current.equals("CREATED") || current.equals("APPROVED")) {
            result.add("COMPLETED");
            if (current.equals("CREATED")) {
                result.add("REJECTED");
            }
            result.add("CANCELLED");
        }
        return result;
    }

    public void changeStatus(ServerRequestResponse request, String status) throws IOException, InterruptedException {
        if (!allowedStatuses(request.status()).contains(status)) {
            throw new IllegalArgumentException("Этот переход статуса недоступен.");
        }
        api.updateStatus(request.id(), status);
    }

    public StatisticsResponse statistics() throws IOException, InterruptedException {
        return api.getStatistics();
    }

    public List<ServerRequestResponse> filter(List<ServerRequestResponse> source, String search,
                                              String status, String os, String minCpu) {
        int minimum = 0;
        if (!minCpu.isBlank()) {
            minimum = Validation.positiveInteger(minCpu, "Минимум CPU");
        }
        String text = search.trim().toLowerCase(Locale.ROOT);
        List<ServerRequestResponse> result = new ArrayList<>();
        for (ServerRequestResponse request : source) {
            String searchable = request.os() + " " + request.status() + " " + Display.status(request.status())
                    + " " + request.cpuCores() + " " + request.ramGb() + " " + request.diskGb();
            boolean matchesSearch = searchable.toLowerCase(Locale.ROOT).contains(text);
            boolean matchesStatus = status.equals("Все") || request.status().equals(status);
            boolean matchesOs = os.equals("Все") || request.os().equalsIgnoreCase(os);
            if (matchesSearch && matchesStatus && matchesOs && request.cpuCores() >= minimum) {
                result.add(request);
            }
        }
        return result;
    }
}
