package dev.hostapp.desktop.service;

import dev.hostapp.desktop.api.ServerApi;
import dev.hostapp.desktop.dto.server.ServerData;
import dev.hostapp.desktop.dto.server.ServerResponse;
import dev.hostapp.desktop.util.Validation;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ServerService {

    private final ServerApi api;

    public ServerService(ServerApi api) {
        this.api = api;
    }

    public List<ServerResponse> getAll() throws IOException, InterruptedException {
        return api.getAll();
    }

    public ServerResponse get(UUID id) throws IOException, InterruptedException {
        return api.get(id);
    }

    public ServerData formData(String hostname, String cpu, String ram, String disk, String ipv4, String ipv6) {
        return new ServerData(Validation.text(hostname, "Имя сервера", 255),
                addresses(ipv4, false), addresses(ipv6, true),
                Validation.positiveInteger(cpu, "CPU"), Validation.positiveInteger(ram, "RAM"),
                Validation.positiveInteger(disk, "Диск"));
    }

    public void update(ServerResponse server, ServerData data) throws IOException, InterruptedException {
        api.update(server.id(), data);
    }

    public void delete(ServerResponse server) throws IOException, InterruptedException {
        api.delete(server.id());
    }

    private List<String> addresses(String text, boolean ipv6) {
        List<String> result = new ArrayList<>();
        if (text.isBlank()) {
            return result;
        }
        for (String part : text.split(",", -1)) {
            String address = part.trim();
            boolean valid = ipv6 ? validIpv6(address) : validIpv4(address);
            if (!valid) {
                throw new IllegalArgumentException("Неверный IPv" + (ipv6 ? "6" : "4") + ": " + address
                        + ". Укажите адреса через запятую.");
            }
            if (!result.contains(address)) {
                result.add(address);
            }
        }
        return result;
    }

    private boolean validIpv4(String address) {
        String[] parts = address.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            if (!part.matches("[0-9]{1,3}") || Integer.parseInt(part) > 255) {
                return false;
            }
        }
        return true;
    }

    private boolean validIpv6(String address) {
        if (!address.contains(":") || !address.matches("[0-9a-fA-F:.]+")) {
            return false;
        }
        try {
            return InetAddress.getByName(address) instanceof Inet6Address;
        } catch (UnknownHostException exception) {
            return false;
        }
    }
}
