package dev.hostapp.desktop;

import dev.hostapp.desktop.dto.ServerRequestResponse;
import dev.hostapp.desktop.service.ExportService;
import dev.hostapp.desktop.service.RequestService;
import dev.hostapp.desktop.service.ServerService;
import dev.hostapp.desktop.util.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClientRulesTest {

    private final RequestService requests = new RequestService(null);

    @Test
    void validatesNumbersRequiredFieldsAndAdminLogin() {
        assertEquals(4, Validation.positiveInteger(" 4 ", "CPU"));
        for (String invalid : List.of("", "0", "-1", "1.5", "abc", "2147483648")) {
            assertThrows(IllegalArgumentException.class, () -> Validation.positiveInteger(invalid, "CPU"));
        }
        assertThrows(IllegalArgumentException.class, () -> requests.formData("4", "8", "20", " "));
        assertThrows(IllegalArgumentException.class, () -> Validation.text("a".repeat(256), "ОС", 255));
        assertEquals("ADMIN", Validation.email(" ADMIN "));
        assertEquals("user@example.org", Validation.email("user@example.org"));
        assertThrows(IllegalArgumentException.class, () -> Validation.email("user"));
        assertThrows(IllegalArgumentException.class, () -> Validation.password(" "));
        assertEquals(" password ", Validation.password(" password "));
    }

    @Test
    void combinesSearchStatusOsAndMinimumCpu() {
        ServerRequestResponse first = request("Ubuntu", "CREATED", 4);
        ServerRequestResponse second = request("Ubuntu", "COMPLETED", 8);
        ServerRequestResponse third = request("Debian", "CREATED", 2);
        List<ServerRequestResponse> source = List.of(first, second, third);
        assertEquals(List.of(first), requests.filter(source, "создана", "CREATED", "Ubuntu", "4"));
        assertEquals(List.of(second), requests.filter(source, "ubuntu", "Все", "Все", "8"));
        assertEquals(List.of(third), requests.filter(source, "Debian", "Все", "Все", ""));
        assertTrue(requests.filter(source, "Ubuntu", "REJECTED", "Все", "").isEmpty());
        assertEquals(source, requests.filter(source, "", "Все", "Все", ""));
        assertThrows(IllegalArgumentException.class, () -> requests.filter(source, "", "Все", "Все", "0"));
    }

    @Test
    void allowsOnlyBackendStatusTransitions() {
        assertEquals(List.of("COMPLETED", "REJECTED", "CANCELLED"), requests.allowedStatuses("CREATED"));
        assertEquals(List.of("COMPLETED", "CANCELLED"), requests.allowedStatuses("APPROVED"));
        for (String status : List.of("COMPLETED", "CANCELLED", "REJECTED")) {
            assertTrue(requests.allowedStatuses(status).isEmpty());
        }
        assertThrows(IllegalArgumentException.class, () -> requests.delete(request("Ubuntu", "COMPLETED", 4)));
        assertThrows(IllegalArgumentException.class,
                () -> requests.changeStatus(request("Ubuntu", "CREATED", 4), "APPROVED"));
    }

    @Test
    void validatesServerAddressesAndResources() {
        ServerService servers = new ServerService(null);
        var data = servers.formData(" host ", "4", "8", "20", "127.0.0.1, 127.0.0.1", "2001:db8::1");
        assertEquals("host", data.hostname());
        assertEquals(List.of("127.0.0.1"), data.ipv4Addresses());
        assertEquals(List.of("2001:db8::1"), data.ipv6Addresses());
        for (String address : List.of("256.0.0.1", "localhost", "1.2.3", "1.2.3.4,")) {
            assertThrows(IllegalArgumentException.class,
                    () -> servers.formData("host", "4", "8", "20", address, ""));
        }
        assertThrows(IllegalArgumentException.class,
                () -> servers.formData("host", "4", "8", "20", "", "example.org"));
        assertThrows(IllegalArgumentException.class,
                () -> servers.formData("host", "0", "8", "20", "", ""));
    }

    @Test
    void exportsOnlyProvidedRowsAndEscapesCsv(@TempDir Path directory) throws Exception {
        ServerRequestResponse first = request("Ubuntu; \"Server\"\nLTS", "CREATED", 4);
        ServerRequestResponse second = request("=1+1", "CREATED", 8);
        Path file = directory.resolve("requests.csv");
        new ExportService().exportRequests(file, List.of(first, second));
        String csv = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("\uFEFFОС;CPU"));
        assertTrue(csv.contains("\"Ubuntu; \"\"Server\"\"\nLTS\";4;"));
        assertTrue(csv.contains("\"'=1+1\";8;"));
        assertTrue(csv.contains(first.id().toString()));
        new ExportService().exportRequests(file, List.of(second));
        assertFalse(Files.readString(file).contains(first.id().toString()));
        new ExportService().exportRequests(file, List.of());
        assertEquals(1, Files.readString(file).lines().count());
    }

    private ServerRequestResponse request(String os, String status, int cpu) {
        return new ServerRequestResponse(UUID.randomUUID(), UUID.randomUUID(), cpu, 8, 20, os, status,
                null, "2026-10-21T12:00:00Z", "2026-10-21T12:00:00Z");
    }
}
