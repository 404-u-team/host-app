package dev.hostapp.backend.service;

import dev.hostapp.backend.model.Server;
import dev.hostapp.backend.model.ServerRequest;
import dev.hostapp.backend.model.User;
import dev.hostapp.backend.repository.DeviceRepository;
import dev.hostapp.backend.repository.ServerRepository;
import dev.hostapp.backend.repository.ServerRequestRepository;
import dev.hostapp.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

class DemoDataCleanupTest {
    private final UserRepository users = mock(UserRepository.class);
    private final DeviceRepository devices = mock(DeviceRepository.class);
    private final ServerRequestRepository requests = mock(ServerRequestRepository.class);
    private final ServerRepository servers = mock(ServerRepository.class);
    private final ServerRequestService requestService = mock(ServerRequestService.class);
    private final DemoDataCleanup cleanup = new DemoDataCleanup(users, devices, requests, servers, requestService);

    @Test
    void deletesSeededAccountsWithRequestsSessionsAndUnsharedServers() {
        User demo = new User("Demo", "User 1", "demo1@hostapp.local", "hash");
        Server server = new Server("demo-server", 2, 4, 50, "Ubuntu");
        ReflectionTestUtils.setField(server, "id", UUID.randomUUID());
        ServerRequest request = new ServerRequest(demo, 2, 4, 50, "Ubuntu");
        ReflectionTestUtils.setField(request, "id", UUID.randomUUID());
        request.setServer(server);
        when(users.findByEmail(demo.getEmail())).thenReturn(Optional.of(demo));
        when(requests.findAllByOwner(demo)).thenReturn(List.of(request));

        cleanup.run(null);

        verify(requestService).delete(request.getId(), demo, true);
        verify(devices).deleteAllByUser(demo);
        verify(users).delete(demo);
        verify(servers).delete(server);
    }

    @Test
    void keepsServersStillReferencedByOtherUsers() {
        User demo = new User("Demo", "User 2", "demo2@hostapp.local", "hash");
        Server server = new Server("shared", 4, 8, 100, "Debian");
        ReflectionTestUtils.setField(server, "id", UUID.randomUUID());
        ServerRequest request = new ServerRequest(demo, 2, 4, 50, "Debian");
        request.setServer(server);
        when(users.findByEmail(demo.getEmail())).thenReturn(Optional.of(demo));
        when(requests.findAllByOwner(demo)).thenReturn(List.of(request));
        when(requests.existsByServer_Id(server.getId())).thenReturn(true);

        cleanup.run(null);

        verify(servers, never()).delete(any());
    }

    @Test
    void doesNotDeleteUnrelatedAccountWithSameEmail() {
        User realUser = new User("Real", "User", "demo1@hostapp.local", "hash");
        when(users.findByEmail(realUser.getEmail())).thenReturn(Optional.of(realUser));

        cleanup.run(null);

        verify(users, never()).delete(any());
        verifyNoInteractions(devices, requestService, servers);
    }

    @Test
    void emptyDatabaseDoesNotGenerateData() {
        cleanup.run(null);
        verify(users, never()).save(any());
        verifyNoInteractions(devices, requestService, servers);
    }
}
