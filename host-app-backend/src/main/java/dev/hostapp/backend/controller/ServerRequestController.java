package dev.hostapp.backend.controller;

import dev.hostapp.backend.dto.serverrequest.CreateServerRequest;
import dev.hostapp.backend.dto.serverrequest.ServerRequestResponse;
import dev.hostapp.backend.dto.serverrequest.StatisticsResponse;
import dev.hostapp.backend.dto.serverrequest.UpdateServerRequest;
import dev.hostapp.backend.model.ServerRequest;
import dev.hostapp.backend.model.User;
import dev.hostapp.backend.service.ServerRequestService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/requests")
public class ServerRequestController {

    private final ServerRequestService requestService;

    public ServerRequestController(ServerRequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping("/create")
    public ResponseEntity<UUID> create(
            @RequestAttribute("user") User currentUser,
            @Valid @RequestBody CreateServerRequest dto
    ) {
        ServerRequestResponse response = requestService.create(currentUser, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response.id());
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ServerRequestResponse> get(
            @RequestAttribute("user") User currentUser,
            @PathVariable UUID uuid
    ) {
        return ResponseEntity.ok(requestService.get(uuid, currentUser, isAdmin(currentUser)));
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ServerRequestResponse> update(
            @RequestAttribute("user") User currentUser,
            @PathVariable UUID uuid,
            @Valid @RequestBody UpdateServerRequest dto
    ) {
        return ResponseEntity.ok(requestService.update(uuid, currentUser, dto, isAdmin(currentUser)));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> delete(
            @RequestAttribute("user") User currentUser,
            @PathVariable UUID uuid
    ) {
        requestService.delete(uuid, currentUser, isAdmin(currentUser));
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<ServerRequestResponse>> getMyRequests(
            @RequestAttribute("user") User currentUser
    ) {
        return ResponseEntity.ok(requestService.getAllByOwner(currentUser));
    }

    @GetMapping("/admin")
    public ResponseEntity<List<ServerRequestResponse>> getAllForAdmin(
            @RequestAttribute("user") User currentUser
    ) {
        if (!isAdmin(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(requestService.getAll());
    }

    @GetMapping("/all")
    public ResponseEntity<Page<ServerRequestResponse>> getAll(
            @RequestAttribute("user") User currentUser,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        if (!isAdmin(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(requestService.getAll(pageable));
    }

    @PatchMapping("/{uuid}/status")
    public ResponseEntity<ServerRequestResponse> updateStatus(
            @RequestAttribute("user") User currentUser,
            @PathVariable UUID uuid,
            @RequestParam ServerRequest.RequestStatus status
    ) {
        if (!isAdmin(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(requestService.updateStatus(uuid, status));
    }

    private boolean isAdmin(User user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getEmail());
    }

    @GetMapping("/search")
    public ResponseEntity<List<ServerRequestResponse>> searchByOs(
            @RequestAttribute("user") User currentUser,
            @RequestParam String os
    ) {
        return ResponseEntity.ok(requestService.searchByOs(currentUser, os, isAdmin(currentUser)));
    }

    @GetMapping("/search/cpu")
    public ResponseEntity<List<ServerRequestResponse>> searchByMinCpu(
            @RequestAttribute("user") User currentUser,
            @RequestParam int minCpu
    ) {
        return ResponseEntity.ok(requestService.searchByMinCpu(currentUser, minCpu, isAdmin(currentUser)));
    }

    @GetMapping("/filter/status")
    public ResponseEntity<List<ServerRequestResponse>> filterByStatus(
            @RequestAttribute("user") User currentUser,
            @RequestParam ServerRequest.RequestStatus status
    ) {
        return ResponseEntity.ok(requestService.filterByStatus(currentUser, status, isAdmin(currentUser)));
    }

    @GetMapping("/filter/os")
    public ResponseEntity<List<ServerRequestResponse>> filterByOs(
            @RequestAttribute("user") User currentUser,
            @RequestParam String os
    ) {
        return ResponseEntity.ok(requestService.filterByOs(currentUser, os, isAdmin(currentUser)));
    }

    @GetMapping("/sort")
    public ResponseEntity<List<ServerRequestResponse>> sort(
            @RequestAttribute("user") User currentUser,
            @RequestParam String by
    ) {
        return ResponseEntity.ok(requestService.sortBy(currentUser, by, isAdmin(currentUser)));
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics(@RequestAttribute("user") User currentUser) {
        return ResponseEntity.ok(requestService.getStatistics(currentUser, isAdmin(currentUser)));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestAttribute("user") User currentUser) throws IOException {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=server-requests.xlsx")
                .body(requestService.exportToExcel(currentUser, isAdmin(currentUser)));
    }

    @GetMapping("/{uuid}/server")
    public ResponseEntity<UUID> getServerId(
            @RequestAttribute("user") User currentUser,
            @PathVariable UUID uuid
    ) {
        ServerRequestResponse response = requestService.get(uuid, currentUser, isAdmin(currentUser));
        if (response.serverId() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response.serverId());
    }
}
