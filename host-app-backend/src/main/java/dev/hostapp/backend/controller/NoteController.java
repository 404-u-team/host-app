package dev.hostapp.backend.controller;

import dev.hostapp.backend.dto.api.NoteDtos.*;
import dev.hostapp.backend.domain.User;
import dev.hostapp.backend.service.NoteService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/requests/{id}/notes")
@SecurityRequirement(name = "bearer")
public class NoteController {
    private final NoteService notes;

    public NoteController(NoteService notes) { this.notes = notes; }

    @GetMapping
    public List<View> list(@RequestAttribute("user") User user, @PathVariable UUID id) {
        return notes.list(user, id);
    }

    @PostMapping
    public ResponseEntity<View> create(@RequestAttribute("user") User user, @PathVariable UUID id,
                                      @Valid @RequestBody Input input) {
        return ResponseEntity.status(201).body(notes.create(user, id, input));
    }

    @PutMapping("/{noteId}")
    public View update(@RequestAttribute("user") User user, @PathVariable UUID id, @PathVariable UUID noteId,
                       @Valid @RequestBody Input input) {
        return notes.update(user, id, noteId, input);
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> delete(@RequestAttribute("user") User user, @PathVariable UUID id,
                                       @PathVariable UUID noteId) {
        notes.delete(user, id, noteId);
        return ResponseEntity.noContent().build();
    }
}
