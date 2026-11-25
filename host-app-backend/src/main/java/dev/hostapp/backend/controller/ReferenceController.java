package dev.hostapp.backend.controller;

import dev.hostapp.backend.dto.api.ReferenceDtos.*;
import dev.hostapp.backend.domain.User;
import dev.hostapp.backend.service.ReferenceService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/references")
@SecurityRequirement(name = "bearer")
public class ReferenceController {
    private final ReferenceService references;

    public ReferenceController(ReferenceService references) { this.references = references; }

    @GetMapping
    public List<View> list(@RequestAttribute("user") User user,
                           @RequestParam(defaultValue = "false") boolean includeDisabled) {
        return references.list(includeDisabled && user.isAdmin());
    }

    @PostMapping
    public ResponseEntity<View> create(@RequestAttribute("user") User user, @Valid @RequestBody Create input) {
        return ResponseEntity.status(201).body(references.create(user, input));
    }

    @PutMapping("/{id}")
    public View update(@RequestAttribute("user") User user, @PathVariable UUID id,
                       @Valid @RequestBody Update input) {
        return references.update(user, id, input);
    }
}
