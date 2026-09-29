package dev.hostapp.backend.service;

import dev.hostapp.backend.dto.CreateServerRequest;
import dev.hostapp.backend.dto.ServerRequestResponse;
import dev.hostapp.backend.exception.ForbiddenException;
import dev.hostapp.backend.exception.InsufficientResourcesException;
import dev.hostapp.backend.exception.ResourceNotFoundException;
import dev.hostapp.backend.model.Server;
import dev.hostapp.backend.model.ServerRequest;
import dev.hostapp.backend.model.User;
import dev.hostapp.backend.repository.ServerRepository;
import dev.hostapp.backend.repository.ServerRequestRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServerRequestService {

    private final ServerRequestRepository requestRepository;
    private final ServerRepository serverRepository;

    @Transactional
    public ServerRequestResponse create(User owner, CreateServerRequest dto) {
        // 1. Создаём заявку со статусом CREATED
        ServerRequest request = new ServerRequest(owner, dto.cpuCores, dto.ramGb, dto.diskGb, dto.os);
        request = requestRepository.save(request);

        // 2. Ищем подходящий сервер (Best Fit: минимально подходящий по CPU)
        Server server = findBestFitServer(dto.cpuCores, dto.ramGb, dto.diskGb)
                .orElseThrow(() -> new InsufficientResourcesException(
                        "Нет доступного сервера с ресурсами: CPU=" + dto.cpuCores + ", RAM=" + dto.ramGb + "GB, Disk="
                                + dto.diskGb + "GB"));

        // 3. Резервируем ресурсы на сервере
        server.allocate(dto.cpuCores, dto.ramGb, dto.diskGb);
        server.addRequestToHistory(request.getId());
        serverRepository.save(server);

        // 4. Привязываем заявку к серверу, обновляем статус
        request.setServer(server);
        request.setStatus(ServerRequest.RequestStatus.APPROVED);
        request = requestRepository.save(request);

        return ServerRequestResponse.from(request, server.getId());
    }

    public ServerRequestResponse get(UUID requestId, User currentUser, boolean isAdmin) {
        ServerRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена: " + requestId));

        checkAccess(request, currentUser, isAdmin);

        UUID serverId = request.getServer() != null ? request.getServer().getId() : null;
        return ServerRequestResponse.from(request, serverId);
    }

    @Transactional
    public void delete(UUID requestId, User currentUser, boolean isAdmin) {
        ServerRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена: " + requestId));

        checkAccess(request, currentUser, isAdmin);

        // Если заявка была размещена на сервере — освобождаем ресурсы
        if (request.getServer() != null) {
            Server server = request.getServer();
            server.release(request.getCpuCores(), request.getRamGb(), request.getDiskGb());
            serverRepository.save(server);
        }

        requestRepository.delete(request);
    }

    // === Полезные дополнительные методы ===

    public List<ServerRequestResponse> getAllByOwner(User owner) {
        return requestRepository.findAllByOwner(owner).stream()
                .map(r -> ServerRequestResponse.from(r, r.getServer() != null ? r.getServer().getId() : null))
                .toList();
    }

    public List<ServerRequestResponse> getAllByOwner(UUID ownerId) {
        return requestRepository.findAllByOwnerId(ownerId).stream()
                .map(r -> ServerRequestResponse.from(r, r.getServer() != null ? r.getServer().getId() : null))
                .toList();
    }

    // Админ: все заявки с пагинацией
    public org.springframework.data.domain.Page<ServerRequestResponse> getAll(
            org.springframework.data.domain.Pageable pageable) {
        return requestRepository.findAll(pageable)
                .map(r -> ServerRequestResponse.from(r, r.getServer() != null ? r.getServer().getId() : null));
    }

    // Админ: смена статуса заявки
    @Transactional
    public ServerRequestResponse updateStatus(UUID requestId, ServerRequest.RequestStatus newStatus) {
        ServerRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена: " + requestId));

        // При переходе в CANCELLED/REJECTED — освобождаем ресурсы, если были выделены
        if ((newStatus == ServerRequest.RequestStatus.CANCELLED || newStatus == ServerRequest.RequestStatus.REJECTED)
                && request.getServer() != null) {
            Server server = request.getServer();
            server.release(request.getCpuCores(), request.getRamGb(), request.getDiskGb());
            serverRepository.save(server);
            request.setServer(null);
        }

        request.setStatus(newStatus);
        request = requestRepository.save(request);

        UUID serverId = request.getServer() != null ? request.getServer().getId() : null;
        return ServerRequestResponse.from(request, serverId);
    }

    // Приватные хелперы

    private Optional<Server> findBestFitServer(int cpu, int ram, int disk) {
        // Берём только ON серверы, где хватает ресурсов
        List<Server> candidates = serverRepository.findAllByStatus(Server.ServerStatus.ON).stream()
                .filter(s -> s.canAllocate(cpu, ram, disk))
                .toList();

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        // Наиболее подходящий: минимально подходящий по CPU (чтобы не тратить мощные
        // сервера на малые задачи)
        return candidates.stream()
                .min(Comparator.comparingInt(Server::getAvailableCpuCores));
    }

    private void checkAccess(ServerRequest request, User currentUser, boolean isAdmin) {
        if (!isAdmin && !request.getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Нет доступа к заявке: " + request.getId());
        }
    }
}