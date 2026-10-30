package dev.hostapp.desktop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.hostapp.desktop.api.ApiClient;
import dev.hostapp.desktop.api.ApiException;
import dev.hostapp.desktop.api.AuthApi;
import dev.hostapp.desktop.api.ServerApi;
import dev.hostapp.desktop.api.ServerRequestApi;
import dev.hostapp.desktop.dto.server.ServerData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ApiClientTest {

    private HttpServer server;
    private ApiClient client;
    private final UUID id = UUID.fromString("7da031fe-159d-4ea8-a9ec-49ed3c531e27");
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        client = new ApiClient("http://127.0.0.1:" + server.getAddress().getPort() + "/");
    }

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void keepsLoginCookieAndClearsItOnLogout() throws Exception {
        AtomicReference<String> loginBody = new AtomicReference<>();
        AtomicReference<String> cookie = new AtomicReference<>();
        server.createContext("/auth/login", exchange -> {
            loginBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.getResponseHeaders().add("Set-Cookie", "session=test-token; Path=/; HttpOnly");
            reply(exchange, 200, "{\"id\":\"" + id + "\",\"name\":\"Иван\",\"surname\":\"Иванов\",\"email\":\"ADMIN\"}");
        });
        server.createContext("/servers", exchange -> {
            cookie.set(exchange.getRequestHeaders().getFirst("Cookie"));
            reply(exchange, 200, "[]");
        });
        assertEquals("ADMIN", new AuthApi(client).login("ADMIN", "password").email());
        assertEquals("password", mapper.readTree(loginBody.get()).get("password").asText());
        new ServerApi(client).getAll();
        assertTrue(cookie.get().contains("session=test-token"));
        client.clearSession();
        new ServerApi(client).getAll();
        assertNull(cookie.get());
    }

    @Test
    void registrationAndEmailCodeFollowServerVerificationRequirement() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        server.createContext("/auth/register", exchange -> reply(exchange, 201,
                "{\"id\":\"" + id + "\",\"name\":\"Name\",\"surname\":\"Surname\","
                        + "\"email\":\"user@example.org\",\"verificationRequired\":true}"));
        server.createContext("/auth/verify-email", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            reply(exchange, 200, "{\"id\":\"" + id + "\",\"email\":\"user@example.org\",\"verificationRequired\":false}");
        });
        server.createContext("/auth/resend-email-code", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            reply(exchange, 200, "{\"id\":\"" + id + "\",\"email\":\"user@example.org\",\"verificationRequired\":true}");
        });
        AuthApi api = new AuthApi(client);
        assertTrue(api.register("Name", "Surname", "user@example.org", "password").verificationRequired());
        assertFalse(api.verifyEmail("user@example.org", "012345").verificationRequired());
        assertEquals("012345", mapper.readTree(body.get()).get("code").asText());
        assertFalse(mapper.readTree(body.get()).has("password"));
        assertTrue(api.resendEmailCode("user@example.org", "password").verificationRequired());
        assertEquals("password", mapper.readTree(body.get()).get("password").asText());
    }

    @Test
    void sendsRequestCrudAndStatusUsingExistingApiContract() throws Exception {
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        server.createContext("/requests", exchange -> {
            method.set(exchange.getRequestMethod());
            path.set(exchange.getRequestURI().toString());
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            if (path.get().equals("/requests/create")) {
                reply(exchange, 201, "\"" + id + "\"");
            } else if (method.get().equals("DELETE")) {
                reply(exchange, 204, "");
            } else if (path.get().equals("/requests") || path.get().equals("/requests/admin")) {
                reply(exchange, 200, "[" + requestJson() + "]");
            } else {
                reply(exchange, 200, requestJson());
            }
        });
        ServerRequestApi api = new ServerRequestApi(client);
        assertEquals(id, api.create(4, 8, 20, "Ubuntu"));
        assertEquals("POST", method.get());
        assertEquals(4, mapper.readTree(body.get()).get("cpu_cores").asInt());
        assertEquals("Ubuntu", api.getAll().get(0).os());
        assertEquals(id, api.getAllAdmin().get(0).id());
        assertEquals("/requests/admin", path.get());
        api.update(id, 8, 16, 40, "Debian");
        assertEquals("PUT", method.get());
        assertEquals(16, mapper.readTree(body.get()).get("ram_gb").asInt());
        api.updateStatus(id, "COMPLETED");
        assertEquals("PATCH", method.get());
        assertEquals("/requests/" + id + "/status?status=COMPLETED", path.get());
        api.delete(id);
        assertEquals("DELETE", method.get());
    }

    @Test
    void serializesServerUpdateAndReadsLinkedServer() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        server.createContext("/servers/", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            reply(exchange, 200, "{\"id\":\"" + id + "\",\"hostname\":\"host\",\"os\":\"Ubuntu\","
                    + "\"cpu_cores\":4,\"ram_gb\":8,\"disk_gb\":20,\"ipv4_addresses\":[\"127.0.0.1\"],\"ipv6_addresses\":[]}");
        });
        ServerApi api = new ServerApi(client);
        assertEquals("host", api.get(id).hostname());
        api.update(id, new ServerData("host", List.of("127.0.0.1"), List.of(), 4, 8, 20));
        assertEquals("127.0.0.1", mapper.readTree(body.get()).get("ipv4_addresses").get(0).asText());
    }

    @Test
    void reportsHttpErrorsWithoutLeakingHtmlOrFollowingRedirects() {
        server.createContext("/forbidden", exchange -> reply(exchange, 403, "<html>proxy error</html>"));
        server.createContext("/missing", exchange -> reply(exchange, 404, "{\"message\":\"Заявка не найдена\"}"));
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", "/missing");
            reply(exchange, 302, "");
        });
        ApiException forbidden = assertThrows(ApiException.class, () -> client.get("/forbidden", String.class));
        assertEquals(403, forbidden.getStatusCode());
        assertFalse(forbidden.getMessage().contains("html"));
        assertEquals("Заявка не найдена",
                assertThrows(ApiException.class, () -> client.get("/missing", String.class)).getMessage());
        assertEquals(302, assertThrows(ApiException.class, () -> client.get("/redirect", String.class)).getStatusCode());
    }

    private String requestJson() {
        return "{\"id\":\"" + id + "\",\"owner_id\":\"" + id + "\",\"os\":\"Ubuntu\","
                + "\"cpu_cores\":4,\"ram_gb\":8,\"disk_gb\":20,\"status\":\"CREATED\","
                + "\"server_id\":null,\"created_at\":\"2026-10-21T12:00:00Z\",\"updated_at\":\"2026-10-21T12:00:00Z\"}";
    }

    private void reply(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, status == 204 ? -1 : bytes.length);
        if (status != 204) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }
}
