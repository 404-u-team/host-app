package dev.hostapp.backend.dto.api;

import dev.hostapp.backend.domain.OsReference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class ReferenceDtos {
    private ReferenceDtos() {}

    public record Create(@NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]{1,64}") String code,
                         @NotBlank @Size(max = 255) String name, @NotNull Boolean enabled) {}
    public record Update(@NotBlank @Size(max = 255) String name, @NotNull Boolean enabled) {}
    public record View(UUID id, String code, String name, boolean enabled) {
        public static View from(OsReference reference) {
            return new View(reference.getId(), reference.getCode(), reference.getName(), reference.isEnabled());
        }
    }
}
