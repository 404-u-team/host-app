package dev.hostapp.backend.service;

import dev.hostapp.backend.dto.serverrequest.CreateServerRequest;
import dev.hostapp.backend.dto.serverrequest.ServerRequestResponse;
import dev.hostapp.backend.dto.serverrequest.StatisticsResponse;
import dev.hostapp.backend.dto.serverrequest.UpdateServerRequest;
import dev.hostapp.backend.exceptions.ForbiddenException;
import dev.hostapp.backend.exceptions.ResourceNotFoundException;
import dev.hostapp.backend.model.Server;
import dev.hostapp.backend.model.ServerRequest;
import dev.hostapp.backend.model.User;
import dev.hostapp.backend.repository.ServerRepository;
import dev.hostapp.backend.repository.ServerRequestRepository;
import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ServerRequestService {

    private final ServerRequestRepository requestRepository;
    private final ServerRepository serverRepository;

    public ServerRequestService(
            ServerRequestRepository requestRepository,
            ServerRepository serverRepository
    ) {
        this.requestRepository = requestRepository;
        this.serverRepository = serverRepository;
    }

    @Transactional
    public ServerRequestResponse create(User owner, CreateServerRequest dto) {
        validateResources(dto.cpuCores(), dto.ramGb(), dto.diskGb(), dto.os());

        ServerRequest request = new ServerRequest(owner, dto.cpuCores(), dto.ramGb(), dto.diskGb(), dto.os().trim());
        request = requestRepository.save(request);
        return ServerRequestResponse.from(request, null);
    }

    public ServerRequestResponse get(UUID requestId, User currentUser, boolean isAdmin) {
        ServerRequest request = findRequest(requestId);
        checkAccess(request, currentUser, isAdmin);
        return toResponse(request);
    }

    @Transactional
    public ServerRequestResponse update(UUID requestId, User currentUser, UpdateServerRequest dto, boolean isAdmin) {
        ServerRequest request = findRequestForUpdate(requestId);
        checkAccess(request, currentUser, isAdmin);
        if (request.getStatus() != ServerRequest.RequestStatus.CREATED) {
            throw new IllegalStateException("Изменить можно только заявку со статусом CREATED");
        }

        validateResources(dto.cpuCores(), dto.ramGb(), dto.diskGb(), dto.os());
        request.setCpuCores(dto.cpuCores());
        request.setRamGb(dto.ramGb());
        request.setDiskGb(dto.diskGb());
        request.setOs(dto.os().trim());
        return toResponse(requestRepository.save(request));
    }

    @Transactional
    public void delete(UUID requestId, User currentUser, boolean isAdmin) {
        ServerRequest request = findRequestForUpdate(requestId);
        checkAccess(request, currentUser, isAdmin);
        if (request.getStatus() == ServerRequest.RequestStatus.COMPLETED) {
            throw new IllegalStateException("Нельзя удалить выполненную заявку");
        }

        requestRepository.delete(request);
    }

    public List<ServerRequestResponse> getAllByOwner(User owner) {
        return requestRepository.findAllByOwner(owner).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ServerRequestResponse> getAll() {
        return requestRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public Page<ServerRequestResponse> getAll(Pageable pageable) {
        return requestRepository.findAll(pageable).map(this::toResponse);
    }

    public List<ServerRequestResponse> searchByOs(User owner, String os, boolean isAdmin) {
        List<ServerRequest> requests = isAdmin
                ? requestRepository.findAllByOsContainingIgnoreCase(os)
                : requestRepository.findAllByOwnerAndOsContainingIgnoreCase(owner, os);
        return requests.stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ServerRequestResponse> searchByMinCpu(User owner, int minCpu, boolean isAdmin) {
        if (minCpu <= 0) {
            throw new IllegalArgumentException("CPU должен быть больше 0");
        }
        List<ServerRequest> requests = isAdmin
                ? requestRepository.findAllByCpuCoresGreaterThanEqual(minCpu)
                : requestRepository.findAllByOwnerAndCpuCoresGreaterThanEqual(owner, minCpu);
        return requests.stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ServerRequestResponse> filterByStatus(User owner, ServerRequest.RequestStatus status, boolean isAdmin) {
        List<ServerRequest> requests = isAdmin
                ? requestRepository.findAllByStatus(status)
                : requestRepository.findAllByOwnerAndStatus(owner, status);
        return requests.stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ServerRequestResponse> filterByOs(User owner, String os, boolean isAdmin) {
        return searchByOs(owner, os, isAdmin);
    }

    public List<ServerRequestResponse> sortBy(User owner, String sortBy, boolean isAdmin) {
        Comparator<ServerRequest> comparator = switch (sortBy) {
            case "date" -> Comparator.comparing(ServerRequest::getCreatedAt);
            case "cpu" -> Comparator.comparing(ServerRequest::getCpuCores);
            case "ram" -> Comparator.comparing(ServerRequest::getRamGb);
            default -> throw new IllegalArgumentException("Сортировка должна быть date, cpu или ram");
        };

        List<ServerRequest> requests = isAdmin
                ? requestRepository.findAll()
                : requestRepository.findAllByOwner(owner);
        return requests.stream()
                .sorted(comparator)
                .map(this::toResponse)
                .toList();
    }

    public StatisticsResponse getStatistics(User owner, boolean isAdmin) {
        List<ServerRequest> requests = isAdmin
                ? requestRepository.findAll()
                : requestRepository.findAllByOwner(owner);
        return new StatisticsResponse(
                requests.size(),
                countByStatus(requests, ServerRequest.RequestStatus.CREATED),
                countByStatus(requests, ServerRequest.RequestStatus.APPROVED),
                countByStatus(requests, ServerRequest.RequestStatus.REJECTED),
                countByStatus(requests, ServerRequest.RequestStatus.COMPLETED),
                countByStatus(requests, ServerRequest.RequestStatus.CANCELLED)
        );
    }

    public byte[] exportToExcel(User owner, boolean isAdmin) throws IOException {
        List<ServerRequest> requests = isAdmin
                ? requestRepository.findAll()
                : requestRepository.findAllByOwner(owner);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Server requests");
            Row header = sheet.createRow(0);
            String[] columns = {"ID", "Owner", "CPU", "RAM", "Disk", "OS", "Status", "Created at"};
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            for (int i = 0; i < requests.size(); i++) {
                ServerRequest request = requests.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(request.getId().toString());
                row.createCell(1).setCellValue(request.getOwner().getEmail());
                row.createCell(2).setCellValue(request.getCpuCores());
                row.createCell(3).setCellValue(request.getRamGb());
                row.createCell(4).setCellValue(request.getDiskGb());
                row.createCell(5).setCellValue(request.getOs());
                row.createCell(6).setCellValue(request.getStatus().name());
                row.createCell(7).setCellValue(request.getCreatedAt().toString());
            }

            workbook.write(output);
            return output.toByteArray();
        }
    }

    @Transactional
    public ServerRequestResponse updateStatus(UUID requestId, ServerRequest.RequestStatus newStatus) {
        ServerRequest request = findRequestForUpdate(requestId);
        if (!canChangeStatus(request.getStatus(), newStatus)) {
            throw new IllegalStateException("Недопустимый переход статуса: " + request.getStatus() + " -> " + newStatus);
        }

        if (newStatus == ServerRequest.RequestStatus.COMPLETED && request.getServer() == null) {
            Server server = new Server(
                    "server-" + request.getId(),
                    request.getCpuCores(),
                    request.getRamGb(),
                    request.getDiskGb(),
                    request.getOs()
            );
            request.setServer(serverRepository.save(server));
        }

        if (newStatus == ServerRequest.RequestStatus.CANCELLED && request.getServer() != null) {
            request.setServer(null);
        }

        request.setStatus(newStatus);
        return toResponse(requestRepository.save(request));
    }

    private ServerRequest findRequestForUpdate(UUID requestId) {
        return requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена: " + requestId));
    }

    private ServerRequest findRequest(UUID requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена: " + requestId));
    }

    private ServerRequestResponse toResponse(ServerRequest request) {
        UUID serverId = request.getServer() != null ? request.getServer().getId() : null;
        return ServerRequestResponse.from(request, serverId);
    }

    private long countByStatus(List<ServerRequest> requests, ServerRequest.RequestStatus status) {
        return requests.stream().filter(request -> request.getStatus() == status).count();
    }

    private void validateResources(Integer cpu, Integer ram, Integer disk, String os) {
        if (cpu == null || cpu <= 0) {
            throw new IllegalArgumentException("CPU должен быть больше 0");
        }
        if (ram == null || ram <= 0) {
            throw new IllegalArgumentException("RAM должен быть больше 0");
        }
        if (disk == null || disk <= 0) {
            throw new IllegalArgumentException("Disk должен быть больше 0");
        }
        if (os == null || os.isBlank()) {
            throw new IllegalArgumentException("OS не должна быть пустой");
        }
    }

    private boolean canChangeStatus(ServerRequest.RequestStatus oldStatus, ServerRequest.RequestStatus newStatus) {
        return switch (oldStatus) {
            case CREATED -> newStatus == ServerRequest.RequestStatus.COMPLETED
                    || newStatus == ServerRequest.RequestStatus.REJECTED
                    || newStatus == ServerRequest.RequestStatus.CANCELLED;
            case APPROVED -> newStatus == ServerRequest.RequestStatus.COMPLETED
                    || newStatus == ServerRequest.RequestStatus.CANCELLED;
            case REJECTED, COMPLETED, CANCELLED -> false;
        };
    }

    private void checkAccess(ServerRequest request, User currentUser, boolean isAdmin) {
        if (currentUser == null || (!isAdmin && !request.getOwner().getId().equals(currentUser.getId()))) {
            throw new ForbiddenException("Нет доступа к заявке: " + request.getId());
        }
    }
}
