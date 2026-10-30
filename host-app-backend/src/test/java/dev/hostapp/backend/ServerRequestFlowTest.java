package dev.hostapp.backend;

import dev.hostapp.backend.dto.serverrequest.CreateServerRequest;
import dev.hostapp.backend.dto.serverrequest.UpdateServerRequest;
import dev.hostapp.backend.exceptions.ForbiddenException;
import dev.hostapp.backend.model.ServerRequest.RequestStatus;
import dev.hostapp.backend.model.ServerRequest.RequestType;
import dev.hostapp.backend.model.User;
import dev.hostapp.backend.repository.ServerRepository;
import dev.hostapp.backend.repository.ServerRequestRepository;
import dev.hostapp.backend.repository.UserRepository;
import dev.hostapp.backend.service.AuthService;
import dev.hostapp.backend.service.ServerRequestService;
import dev.hostapp.backend.service.ServerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:server-requests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.mail.host=", "spring.mail.username=", "spring.mail.password=", "app.email.from=",
        "app.email.verification-enabled=false",
        "app.admin.bootstrap-password="
})
class ServerRequestFlowTest {
    @Autowired ServerRequestService requests;
    @Autowired ServerService servers;
    @Autowired ServerRepository serverRepository;
    @Autowired ServerRequestRepository requestRepository;
    @Autowired UserRepository users;
    @Autowired AuthService auth;
    @Autowired Environment env;

    private User owner;
    private UUID serverId;
    private UUID originalId;

    @BeforeEach
    void prepare() {
        owner = users.save(new User("Name", "Surname", UUID.randomUUID() + "@example.org", "unused"));
        var request = requests.create(owner, new CreateServerRequest(2, 4, 20, "Debian"));
        originalId = request.id();
        serverId = requests.updateStatus(originalId, RequestStatus.COMPLETED).serverId();
    }

    @Test
    void changeUpdatesExistingServerOnlyAfterApproval() {
        var before = servers.get(serverId, owner, false);
        var change = requests.requestChange(owner, serverId, new CreateServerRequest(4, 8, 40, "Ubuntu"));
        assertEquals(RequestType.CHANGE, change.type());
        assertEquals(RequestStatus.CREATED, change.status());
        assertEquals(before, servers.get(serverId, owner, false));
        requests.update(change.id(), owner, new UpdateServerRequest(8, 16, 80, "Ubuntu"), false);
        requests.updateStatus(change.id(), RequestStatus.COMPLETED);
        var after = servers.get(serverId, owner, false);
        assertEquals(serverId, after.id());
        assertEquals(before.hostname(), after.hostname());
        assertEquals(before.ipv4Addresses(), after.ipv4Addresses());
        assertEquals(8, after.cpuCores());
        assertEquals(16, after.ramGb());
        assertEquals(80, after.diskGb());
        assertEquals("Ubuntu", after.os());
        assertEquals(2, requests.get(originalId, owner, false).cpuCores());
        assertEquals(1, servers.getAllAccessible(owner, false).size());
        assertThrows(IllegalStateException.class, () -> requests.updateStatus(change.id(), RequestStatus.COMPLETED));
    }

    @Test
    void rejectionOrCancellationDoesNotChangeServerAndRetainsLinks() {
        var before = servers.get(serverId, owner, false);
        var change = requests.requestChange(owner, serverId, new CreateServerRequest(4, 8, 40, "Ubuntu"));
        requests.updateStatus(change.id(), RequestStatus.REJECTED);
        var cancellation = requests.requestCancellation(owner, serverId);
        requests.updateStatus(cancellation.id(), RequestStatus.CANCELLED);
        var rejected = requests.requestCancellation(owner, serverId);
        requests.updateStatus(rejected.id(), RequestStatus.REJECTED);
        assertEquals(before, servers.get(serverId, owner, false));
        assertEquals(serverId, requests.get(change.id(), owner, false).serverId());
        assertEquals(serverId, requests.get(cancellation.id(), owner, false).serverId());
        servers.delete(serverId);
        assertTrue(requests.getAllByOwner(owner).isEmpty());
    }

    @Test
    void approvedCancellationDeletesServerAndEveryRelatedRequest() {
        var unrelated = requests.create(owner, new CreateServerRequest(1, 2, 10, "Debian"));
        var change = requests.requestChange(owner, serverId, new CreateServerRequest(4, 8, 40, "Ubuntu"));
        requests.updateStatus(change.id(), RequestStatus.REJECTED);
        requests.requestChange(owner, serverId, new CreateServerRequest(8, 16, 80, "Ubuntu"));
        var cancellation = requests.requestCancellation(owner, serverId);
        assertEquals(RequestType.CANCEL, cancellation.type());
        assertEquals(2, cancellation.cpuCores());
        assertTrue(serverRepository.existsById(serverId));
        assertThrows(IllegalStateException.class, () -> requests.update(cancellation.id(), owner,
                new UpdateServerRequest(4, 8, 40, "Ubuntu"), false));
        var result = requests.updateStatus(cancellation.id(), RequestStatus.COMPLETED);
        assertEquals(RequestStatus.COMPLETED, result.status());
        assertFalse(serverRepository.existsById(serverId));
        assertFalse(requestRepository.existsById(originalId));
        assertFalse(requestRepository.existsById(change.id()));
        assertFalse(requestRepository.existsById(cancellation.id()));
        assertEquals(java.util.List.of(unrelated.id()), requests.getAllByOwner(owner).stream().map(r -> r.id()).toList());
    }

    @Test
    void otherUsersCannotRequestChangesOrCancellationAndInvalidResourcesAreRejected() {
        User other = users.save(new User("Other", "User", UUID.randomUUID() + "@example.org", "unused"));
        assertThrows(ForbiddenException.class, () -> requests.requestChange(other, serverId,
                new CreateServerRequest(4, 8, 40, "Ubuntu")));
        assertThrows(ForbiddenException.class, () -> requests.requestCancellation(other, serverId));
        assertThrows(IllegalArgumentException.class, () -> requests.requestChange(owner, serverId,
                new CreateServerRequest(0, 8, 40, "Ubuntu")));
    }

    @Test
    void httpEndpointsCreateRequestsAndOnlyAdminCanProcessThem() throws Exception {
        User user = auth.registerUser("Name", "Surname", UUID.randomUUID() + "@example.org", "password");
        String token = auth.loginUser(user.getEmail(), "password", "ip", "test");
        var original = requests.create(user, new CreateServerRequest(2, 4, 20, "Debian"));
        UUID id = requests.updateStatus(original.id(), RequestStatus.COMPLETED).serverId();
        String path = "/requests/server/" + id;
        var change = send("POST", path + "/change", token,
                "{\"cpu_cores\":4,\"ram_gb\":8,\"disk_gb\":40,\"os\":\"Ubuntu\"}");
        assertEquals(201, change.statusCode());
        String changeId = change.body().replace("\"", "");
        assertEquals(RequestType.CHANGE, requests.get(UUID.fromString(changeId), user, false).type());
        assertEquals(403, send("PATCH", "/requests/" + changeId + "/status?status=COMPLETED", token, "").statusCode());
        assertEquals(400, send("POST", path + "/change", token, "{}").statusCode());
        assertEquals(403, send("POST", "/requests/server/" + serverId + "/cancel", token, "").statusCode());
        var cancellation = send("POST", path + "/cancel", token, "");
        assertEquals(201, cancellation.statusCode());
        assertEquals(RequestType.CANCEL, requests.get(UUID.fromString(cancellation.body().replace("\"", "")), user, false).type());
        assertEquals(401, send("POST", path + "/cancel", null, "").statusCode());
        users.save(new User("Admin", "User", "ADMIN", user.getPasswordHash()));
        String adminToken = auth.loginUser("ADMIN", "password", "ip", "test");
        assertEquals(200, send("PATCH", "/requests/" + changeId + "/status?status=COMPLETED", adminToken, "").statusCode());
        assertEquals("Ubuntu", servers.get(id, user, false).os());
    }

    private HttpResponse<String> send(String method, String path, String token, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + env.getProperty("local.server.port") + path))
                .header("Content-Type", "application/json");
        if (token != null) builder.header("Cookie", "session=" + token);
        return HttpClient.newHttpClient().send(builder.method(method, HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
