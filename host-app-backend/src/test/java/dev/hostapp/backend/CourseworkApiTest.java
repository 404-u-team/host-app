package dev.hostapp.backend;

import dev.hostapp.backend.domain.OsReference;
import dev.hostapp.backend.domain.Role;
import dev.hostapp.backend.domain.User;
import dev.hostapp.backend.repository.OsReferenceRepository;
import dev.hostapp.backend.repository.UserRepository;
import dev.hostapp.backend.security.Passwords;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:coursework;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.mail.host="
})
class CourseworkApiTest {
    @LocalServerPort int port;
    @Autowired UserRepository users;
    @Autowired OsReferenceRepository references;
    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void combinedFilterRespectsOwnerAndVersion() throws Exception {
        UUID reference = references.save(new OsReference("ALPINE", "Alpine", true)).getId();
        String editor = login(addUser());
        String other = login(addUser());
        String data = """
                {"title":"Special Cloud","description":"Demo","referenceId":"%s",
                 "cpuCores":16,"ramGb":8,"diskGb":100,"urgent":true,"tags":["Study"]}
                """.formatted(reference);
        JsonNode created = body(send("POST", "/api/requests", editor, data), 201);
        String id = created.get("id").asText();
        assertEquals("HIGH", created.get("priority").asText());

        String query = "/api/requests?q=special&priority=HIGH&tag=study&cpu=16&cpuOp=EQ";
        assertEquals(1, body(send("GET", query, editor, ""), 200).get("totalElements").asInt());
        assertEquals(0, body(send("GET", query, other, ""), 200).get("totalElements").asInt());
        assertEquals(400, send("GET", "/api/requests?size=101", editor, "").statusCode());
        assertEquals(400, send("GET", "/api/requests/no-uuid", editor, "").statusCode());

        long version = created.get("version").asLong();
        String path = "/api/requests/" + id;
        JsonNode archived = body(send("DELETE", path + "?version=" + version, editor, ""), 200);
        assertTrue(archived.get("archived").asBoolean());
        assertEquals(409, send("POST", path + "/restore", editor, "{\"version\":" + version + "}").statusCode());
        assertEquals(403, send("DELETE", path + "/permanent", editor, "").statusCode());
    }

    @Test
    void passwordAndRefreshRotationInvalidateOldCredentials() throws Exception {
        String login = addUser();
        JsonNode original = body(send("POST", "/api/auth/login", null, loginBody(login)), 200);
        String initialRefresh = "{\"refreshToken\":\"" + original.get("refreshToken").asText() + "\"}";
        JsonNode rotated = body(send("POST", "/api/auth/refresh", null, initialRefresh), 200);
        assertEquals(401, send("POST", "/api/auth/refresh", null, initialRefresh).statusCode());
        String access = rotated.get("accessToken").asText();
        assertEquals(400, send("POST", "/api/auth/change-password", access, """
                {"currentPassword":"original-password","newPassword":"short"}
                """).statusCode());
        JsonNode changed = body(send("POST", "/api/auth/change-password", access, """
                {"currentPassword":"original-password","newPassword":"changed-password"}
                """), 200);
        assertEquals(401, send("GET", "/api/auth/me", access, "").statusCode());
        assertEquals(200, send("GET", "/api/auth/me", changed.get("accessToken").asText(), "").statusCode());
        String refresh = "{\"refreshToken\":\"" + changed.get("refreshToken").asText() + "\"}";
        assertEquals(204, send("POST", "/api/auth/logout", null, refresh).statusCode());
        assertEquals(401, send("POST", "/api/auth/refresh", null, refresh).statusCode());
    }

    @Test
    void temporaryPasswordsAreAcceptedButNeverReturned() throws Exception {
        String adminLogin = UUID.randomUUID() + "@example.org";
        User admin = new User("Test", "Admin", adminLogin, Passwords.encode("original-password"));
        admin.updateAccount("Test", "Admin", adminLogin, Set.of(Role.ADMIN), true);
        users.save(admin);
        String access = login(adminLogin);

        String newAccount = """
                {"name":"New","surname":"Viewer","login":"%s","roles":["VIEWER"],"active":true}
                """.formatted(UUID.randomUUID() + "@example.org");
        assertEquals(400, send("POST", "/api/admin/users", access, "{\"account\":" + newAccount + "}").statusCode());
        JsonNode created = body(send("POST", "/api/admin/users", access,
                "{\"account\":" + newAccount + ",\"temporaryPassword\":\"first-temporary-password\"}"), 201);
        assertFalse(created.has("temporaryPassword"));
        assertTrue(created.get("mustChangePassword").asBoolean());
        JsonNode reset = body(send("POST", "/api/admin/users/" + created.get("id").asText() + "/reset-password",
                access, "{\"temporaryPassword\":\"second-temporary-password\"}"), 200);
        assertFalse(reset.has("temporaryPassword"));
        assertTrue(reset.get("mustChangePassword").asBoolean());
    }

    @Test
    void browserCanUseBearerForServersAndAdministratorDocumentation() throws Exception {
        String editor = login(addUser());
        assertEquals(401, send("GET", "/servers", null, "").statusCode());
        assertEquals(200, send("GET", "/servers", editor, "").statusCode());
        assertEquals(403, send("POST", "/api/admin/docs/session", editor, "").statusCode());

        String adminLogin = UUID.randomUUID() + "@example.org";
        User admin = new User("Test", "Admin", adminLogin, Passwords.encode("original-password"));
        admin.updateAccount("Test", "Admin", adminLogin, Set.of(Role.ADMIN), true);
        users.save(admin);
        String access = login(adminLogin);
        HttpResponse<String> response = send("POST", "/api/admin/docs/session", access, "");
        assertEquals(204, response.statusCode());
        String cookie = response.headers().firstValue("Set-Cookie").orElseThrow();
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("SameSite=Strict"));
        String value = cookie.split(";")[0];
        assertEquals(200, getWithCookie("/v3/api-docs", value).statusCode());
        assertEquals(401, getWithCookie("/servers", value).statusCode());
    }

    private HttpResponse<String> getWithCookie(String path, String cookie) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Cookie", cookie).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String addUser() {
        String login = UUID.randomUUID() + "@example.org";
        users.save(new User("Test", "User", login, Passwords.encode("original-password")));
        return login;
    }

    private String login(String login) throws Exception {
        return body(send("POST", "/api/auth/login", null, loginBody(login)), 200).get("accessToken").asText();
    }

    private String loginBody(String login) {
        return "{\"login\":\"" + login + "\",\"password\":\"original-password\"}";
    }

    private HttpResponse<String> send(String method, String path, String token, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(body));
        if (token != null) request.header("Authorization", "Bearer " + token);
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode body(HttpResponse<String> response, int status) {
        assertEquals(status, response.statusCode(), response.body());
        return json.readTree(response.body());
    }
}
