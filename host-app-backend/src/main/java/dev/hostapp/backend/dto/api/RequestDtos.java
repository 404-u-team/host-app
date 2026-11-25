package dev.hostapp.backend.dto.api;

import dev.hostapp.backend.domain.ServerRequest;
import dev.hostapp.backend.domain.ServerRequest.Priority;
import dev.hostapp.backend.domain.ServerRequest.RequestStatus;
import dev.hostapp.backend.domain.ServerRequest.RequestType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class RequestDtos {
    private RequestDtos() {}

    public static final String TAG = "[\\p{L}\\p{N}_-]{1,48}";
    public static final String EXTERNAL_ID = "[a-zA-Z0-9_-]{1,80}";

    public record Data(@Pattern(regexp = EXTERNAL_ID) String externalId,
                       @NotBlank @Size(max = 160) String title,
                       @NotNull @Size(max = 2000) String description,
                       @NotNull UUID referenceId,
                       @NotNull @Min(1) @Max(10000) Integer cpuCores,
                       @NotNull @Min(1) @Max(1000000) Integer ramGb,
                       @NotNull @Min(1) @Max(10000000) Integer diskGb,
                       @NotNull Boolean urgent,
                       @NotNull @Size(max = 20) Set<@Pattern(regexp = TAG) @NotNull String> tags) {}
    public record Update(@Valid @NotNull Data data, @NotNull @PositiveOrZero Long version) {}
    public record Version(@NotNull @PositiveOrZero Long version) {}

    public record View(UUID id, String externalId, String title, String description,
                       UUID ownerId, UUID referenceId, String referenceCode, String os, UUID serverId,
                       int cpuCores, int ramGb, int diskGb, RequestStatus status, RequestType type,
                       boolean urgent, Priority priority, boolean archived, Set<String> tags,
                       Instant createdAt, Instant updatedAt, long version) {
        public static View from(ServerRequest request) {
            var reference = request.getReference();
            var server = request.getServer();
            return new View(request.getId(), request.getExternalId(), request.getTitle(), request.getDescription(),
                    request.getOwner().getId(), reference == null ? null : reference.getId(),
                    reference == null ? null : reference.getCode(), request.getOs(), server == null ? null : server.getId(),
                    request.getCpuCores(), request.getRamGb(), request.getDiskGb(), request.getStatus(), request.getType(),
                    request.isUrgent(), request.getPriority(), request.isArchived(), request.getTags(),
                    request.getCreatedAt(), request.getUpdatedAt(), request.getVersion());
        }
    }

    public enum BulkAction { ARCHIVE, RESTORE, SET_STATUS, ADD_TAG, REMOVE_TAG }

    public record Bulk(@NotEmpty @Size(max = 100) Set<@NotNull UUID> ids, @NotNull BulkAction action,
                       RequestStatus status, @Pattern(regexp = TAG) String tag) {}
    public record BulkError(UUID id, String message) {}
    public record BulkResult(List<UUID> succeeded, List<BulkError> errors) {}
}
