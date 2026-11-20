package dev.hostapp.backend.controller;

import dev.hostapp.backend.domain.AuditEvent;
import dev.hostapp.backend.dto.api.AccountDtos.*;
import dev.hostapp.backend.dto.api.AuditView;
import dev.hostapp.backend.filter.ApiQuery;
import dev.hostapp.backend.domain.User;
import dev.hostapp.backend.repository.AuditRepository;
import dev.hostapp.backend.service.AdminUserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@SecurityRequirement(name = "bearer")
public class AdminController {
    private final AdminUserService admin;
    private final AuditRepository audit;

    public AdminController(AdminUserService admin, AuditRepository audit) {
        this.admin = admin;
        this.audit = audit;
    }

    @GetMapping("/users")
    public Page<UserView> users(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size) {
        return admin.list(page, size);
    }

    @PostMapping("/users")
    public ResponseEntity<UserView> create(@RequestAttribute("user") User user,
                                          @Valid @RequestBody CreateUser input) {
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(admin.create(user, input));
    }

    @PutMapping("/users/{id}")
    public UserView update(@RequestAttribute("user") User user, @PathVariable UUID id,
                           @Valid @RequestBody UserInput input) {
        return admin.update(user, id, input);
    }

    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<UserView> reset(@RequestAttribute("user") User user, @PathVariable UUID id,
                                         @Valid @RequestBody(required = false) ResetPassword input) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(admin.resetPassword(user, id, input));
    }

    @GetMapping("/audit")
    public Page<AuditView> audit(@RequestParam(defaultValue = "") @Size(max = 200) String q,
                                @RequestParam(defaultValue = "happenedAt") String sort,
                                @RequestParam(defaultValue = "desc") String direction,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size) {
        String pattern = ApiQuery.contains(q);
        Specification<AuditEvent> search = (root, query, cb) -> q.isBlank() ? cb.conjunction() : cb.or(
                cb.like(cb.lower(root.get("actorLogin")), pattern, '!'),
                cb.like(cb.lower(root.get("eventType")), pattern, '!'),
                cb.like(cb.lower(root.get("entityType")), pattern, '!'),
                cb.like(cb.lower(root.get("description")), pattern, '!'),
                cb.like(cb.lower(root.get("correlationId")), pattern, '!'));
        return audit.findAll(search, ApiQuery.page(page, size, ApiQuery.sort(sort, direction,
                Map.of("happenedAt", "happenedAt", "eventType", "eventType", "actorLogin", "actorLogin"))))
                .map(AuditView::from);
    }
}
