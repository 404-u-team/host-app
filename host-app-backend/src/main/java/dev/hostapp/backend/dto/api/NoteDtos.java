package dev.hostapp.backend.dto.api;

import dev.hostapp.backend.domain.RequestNote;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class NoteDtos {
    private NoteDtos() {}

    public record Input(@NotBlank @Size(max = 2000) String body, @NotNull Boolean pinned) {}
    public record View(UUID id, UUID authorId, String body, boolean pinned, Instant createdAt, Instant updatedAt) {
        public static View from(RequestNote note) {
            return new View(note.getId(), note.getAuthorId(), note.getBody(), note.isPinned(),
                    note.getCreatedAt(), note.getUpdatedAt());
        }
    }
}
