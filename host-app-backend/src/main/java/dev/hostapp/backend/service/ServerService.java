package dev.hostapp.backend.service;

import dev.hostapp.backend.dto.server.ServerResponse;
import dev.hostapp.backend.dto.server.UpdateServerRequest;
import dev.hostapp.backend.exceptions.ForbiddenException;
import dev.hostapp.backend.exceptions.ResourceNotFoundException;
import dev.hostapp.backend.model.Server;
import dev.hostapp.backend.model.User;
import dev.hostapp.backend.repository.ServerRepository;
import dev.hostapp.backend.repository.ServerRequestRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ServerService {

    private final ServerRepository serverRepository;
    private final ServerRequestRepository requestRepository;

    public ServerService(ServerRepository serverRepository, ServerRequestRepository requestRepository) {
        this.serverRepository = serverRepository;
        this.requestRepository = requestRepository;
    }

    @Transactional
    public List<ServerResponse> getAllAccessible(User user, boolean isAdmin) {
        List<Server> servers = isAdmin
                ? serverRepository.findAll()
                : requestRepository.findAssignedServersByOwner(user);

        return servers.stream()
                .sorted(Comparator.comparing(Server::getHostname, String.CASE_INSENSITIVE_ORDER))
                .map(ServerResponse::from)
                .toList();
    }

    @Transactional
    public ServerResponse get(UUID serverId, User user, boolean isAdmin) {
        Server server = findServer(serverId);
        checkAccess(server, user, isAdmin);
        return ServerResponse.from(server);
    }

    @Transactional
    public ServerResponse update(UUID serverId, UpdateServerRequest dto) {
        Server server = findServer(serverId);
        String hostname = normalizeHostname(dto.hostname());
        ensureHostnameAvailable(hostname, serverId);

        server.setCpuCores(dto.cpuCores());
        server.setRamGb(dto.ramGb());
        server.setDiskGb(dto.diskGb());
        server.setHostname(hostname);
        server.setIpv4Addresses(normalizeAddresses(dto.ipv4Addresses()));
        server.setIpv6Addresses(normalizeAddresses(dto.ipv6Addresses()));
        return ServerResponse.from(serverRepository.save(server));
    }

    @Transactional
    public void delete(UUID serverId) {
        Server server = findServer(serverId);
        requestRepository.deleteAll(requestRepository.findAllByServer(server));
        requestRepository.flush();
        serverRepository.delete(server);
    }

    private Server findServer(UUID serverId) {
        return serverRepository.findById(serverId)
                .orElseThrow(() -> new ResourceNotFoundException("Сервер не найден: " + serverId));
    }

    private void checkAccess(Server server, User user, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (user == null || !requestRepository.existsByOwner_IdAndServer_Id(user.getId(), server.getId())) {
            throw new ForbiddenException("Нет доступа к серверу: " + server.getId());
        }
    }

    private String normalizeHostname(String hostname) {
        if (hostname == null || hostname.isBlank()) {
            throw new IllegalArgumentException("Имя сервера не должно быть пустым");
        }
        return hostname.trim();
    }

    private void ensureHostnameAvailable(String hostname, UUID currentServerId) {
        Server existing = serverRepository.findByHostname(hostname).orElse(null);
        if (existing != null && !existing.getId().equals(currentServerId)) {
            throw new IllegalArgumentException("Сервер с таким именем уже существует: " + hostname);
        }
    }

    private List<String> normalizeAddresses(List<String> addresses) {
        if (addresses == null) {
            return new ArrayList<>();
        }
        return addresses.stream()
                .filter(address -> address != null && !address.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
