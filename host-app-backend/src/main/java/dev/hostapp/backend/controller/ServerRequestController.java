package dev.hostapp.backend.controller;

import dev.hostapp.backend.dto.CreateServerRequest;
import dev.hostapp.backend.dto.ServerRequestResponse;
import dev.hostapp.backend.model.ServerRequest;
import dev.hostapp.backend.model.User;
import dev.hostapp.backend.service.ServerRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
public class ServerRequestController {

    private final ServerRequestService requestService;

    // POST /requests/create
    @PostMapping("/create")
    public ResponseEntity<UUID> create(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateServerRequest dto
    ) {
        ServerRequestResponse response = requestService.create(currentUser, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response.id);
    }

    // GET /requests/{uuid}
    @GetMapping("/{uuid}")
    public ResponseEntity<ServerRequestResponse> get(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID uuid
    ) {
        // isAdmin можно получать из роли пользователя
        boolean isAdmin = currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getEmail()); // заглушка
        ServerRequestResponse response = requestService.get(uuid, currentUser, isAdmin);
        return ResponseEntity.ok(response);
    }

    // DELETE /requests/{uuid}
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID uuid
    ) {
        boolean isAdmin = currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getEmail()); // заглушка
        requestService.delete(uuid, currentUser, isAdmin);
        return ResponseEntity.noContent().build();
    }

    // === Полезные дополнительные эндпоинты ===

    // GET /requests — мои заявки (с пагинацией)
    @GetMapping
    public ResponseEntity<List<ServerRequestResponse>> getMyRequests(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        List<ServerRequestResponse> requests = requestService.getAllByOwner(currentUser);
        return ResponseEntity.ok(requests);
    }

    // GET /requests/all — все заявки (админ, с пагинацией)
    @GetMapping("/all")
    public ResponseEntity<Page<ServerRequestResponse>> getAll(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        boolean isAdmin = currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getEmail()); // заглушка
        if (!isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(requestService.getAll(pageable));
    }

    // PATCH /requests/{uuid}/status — смена статуса (админ)
    @PatchMapping("/{uuid}/status")
    public ResponseEntity<ServerRequestResponse> updateStatus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID uuid,
            @RequestParam ServerRequest.RequestStatus status
    ) {
        boolean isAdmin = currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getEmail()); // заглушка
        if (!isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        ServerRequestResponse response = requestService.updateStatus(uuid, status);
        return ResponseEntity.ok(response);
    }

    // GET /requests/{uuid}/server — получить сервер, на котором размещена заявка
    @GetMapping("/{uuid}/server")
    public ResponseEntity<UUID> getServerId(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID uuid
    ) {
        boolean isAdmin = currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getEmail()); // заглушка
        ServerRequestResponse response = requestService.get(uuid, currentUser, isAdmin);
        if (response.serverId == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response.serverId);
    }
}