package dev.hostapp.backend.controller;

import dev.hostapp.backend.dto.server.CreateServerRequest;
import dev.hostapp.backend.dto.server.ServerResponse;
import dev.hostapp.backend.dto.server.UpdateServerRequest;
import dev.hostapp.backend.dto.server.UpdateServerStatusRequest;
import dev.hostapp.backend.exceptions.ForbiddenException;
import dev.hostapp.backend.model.User;
import dev.hostapp.backend.service.ServerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/servers")
public class ServerController {

    private final ServerService serverService;

    public ServerController(ServerService serverService) {
        this.serverService = serverService;
    }

    @GetMapping
    public ResponseEntity<List<ServerResponse>> getAll(@RequestAttribute("user") User currentUser) {
        return ResponseEntity.ok(serverService.getAllAccessible(currentUser, isAdmin(currentUser)));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ServerResponse> get(
            @RequestAttribute("user") User currentUser,
            @PathVariable UUID uuid
    ) {
        return ResponseEntity.ok(serverService.get(uuid, currentUser, isAdmin(currentUser)));
    }

    @PostMapping
    public ResponseEntity<ServerResponse> create(
            @RequestAttribute("user") User currentUser,
            @Valid @RequestBody CreateServerRequest dto
    ) {
        requireAdmin(currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(serverService.create(dto));
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ServerResponse> update(
            @RequestAttribute("user") User currentUser,
            @PathVariable UUID uuid,
            @Valid @RequestBody UpdateServerRequest dto
    ) {
        requireAdmin(currentUser);
        return ResponseEntity.ok(serverService.update(uuid, dto));
    }

    @PatchMapping("/{uuid}/status")
    public ResponseEntity<ServerResponse> updateStatus(
            @RequestAttribute("user") User currentUser,
            @PathVariable UUID uuid,
            @Valid @RequestBody UpdateServerStatusRequest dto
    ) {
        requireAdmin(currentUser);
        return ResponseEntity.ok(serverService.updateStatus(uuid, dto.status()));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> delete(
            @RequestAttribute("user") User currentUser,
            @PathVariable UUID uuid
    ) {
        requireAdmin(currentUser);
        serverService.delete(uuid);
        return ResponseEntity.noContent().build();
    }

    private void requireAdmin(User currentUser) {
        if (!isAdmin(currentUser)) {
            throw new ForbiddenException("Управлять списком серверов может только администратор");
        }
    }

    private boolean isAdmin(User currentUser) {
        return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getEmail());
    }
}
