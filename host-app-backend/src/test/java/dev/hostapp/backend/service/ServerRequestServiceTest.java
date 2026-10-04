package dev.hostapp.backend.service;

import dev.hostapp.backend.dto.serverrequest.CreateServerRequest;
import dev.hostapp.backend.model.Server;
import dev.hostapp.backend.model.ServerRequest;
import dev.hostapp.backend.model.User;
import dev.hostapp.backend.repository.ServerRepository;
import dev.hostapp.backend.repository.ServerRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServerRequestServiceTest {
    private final ServerRequestRepository requests = mock(ServerRequestRepository.class);
    private final ServerRepository servers = mock(ServerRepository.class);
    private final ServerRequestService service = new ServerRequestService(requests, servers);
    private final User owner = new User("User", "Owner", "owner@example.org", "hash");
    private ServerRequest request;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(owner, "id", UUID.randomUUID());
        request = new ServerRequest(owner, 4, 8, 100, "Debian 12");
        ReflectionTestUtils.setField(request, "id", UUID.randomUUID());
        when(requests.findByIdForUpdate(request.getId())).thenReturn(Optional.of(request));
        when(requests.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createStartsInCreatedWithoutProvisioning() {
        var response = service.create(owner, new CreateServerRequest(4, 8, 100, " Debian 12 "));
        assertEquals(ServerRequest.RequestStatus.CREATED, response.status());
        assertNull(response.serverId());
        assertEquals("Debian 12", response.os());
        verifyNoInteractions(servers);
    }

    @Test
    void completeProvisionsRequestedServerForOwnerExactlyOnce() {
        UUID serverId = UUID.randomUUID();
        when(servers.save(any())).thenAnswer(invocation -> {
            Server server = invocation.getArgument(0);
            ReflectionTestUtils.setField(server, "id", serverId);
            return server;
        });

        var response = service.updateStatus(request.getId(), ServerRequest.RequestStatus.COMPLETED);
        Server server = request.getServer();
        assertEquals(ServerRequest.RequestStatus.COMPLETED, response.status());
        assertEquals(serverId, response.serverId());
        assertSame(owner, request.getOwner());
        assertEquals(4, server.getCpuCores());
        assertEquals(8, server.getRamGb());
        assertEquals(100, server.getDiskGb());
        assertEquals("Debian 12", server.getOs());
        assertEquals(0, server.getAvailableCpuCores());
        assertEquals(0, server.getAvailableRamGb());
        assertEquals(0, server.getAvailableDiskGb());
        assertEquals(List.of(request.getId()), server.getRequestIdsHistory());

        when(requests.findAssignedServersByOwner(owner)).thenReturn(List.of(server));
        var accessible = new ServerService(servers, requests).getAllAccessible(owner, false);
        assertEquals(serverId, accessible.get(0).id());
        assertEquals("Debian 12", accessible.get(0).os());

        assertThrows(IllegalStateException.class,
                () -> service.updateStatus(request.getId(), ServerRequest.RequestStatus.COMPLETED));
        verify(servers, times(1)).save(any());
        verify(requests, times(1)).save(request);
    }

    @ParameterizedTest
    @EnumSource(value = ServerRequest.RequestStatus.class, names = {"REJECTED", "CANCELLED"})
    void otherAllowedStatusesDoNotProvision(ServerRequest.RequestStatus status) {
        service.updateStatus(request.getId(), status);
        assertEquals(status, request.getStatus());
        assertNull(request.getServer());
        verifyNoInteractions(servers);
    }

    @Test
    void approvedIsUnavailable() {
        assertThrows(IllegalStateException.class,
                () -> service.updateStatus(request.getId(), ServerRequest.RequestStatus.APPROVED));
        assertEquals(ServerRequest.RequestStatus.CREATED, request.getStatus());
        verifyNoInteractions(servers);
        verify(requests, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = ServerRequest.RequestStatus.class, names = {"REJECTED", "CANCELLED", "COMPLETED"})
    void terminalStatusesCannotProvision(ServerRequest.RequestStatus status) {
        request.setStatus(status);
        assertThrows(IllegalStateException.class,
                () -> service.updateStatus(request.getId(), ServerRequest.RequestStatus.COMPLETED));
        verifyNoInteractions(servers);
    }
}
