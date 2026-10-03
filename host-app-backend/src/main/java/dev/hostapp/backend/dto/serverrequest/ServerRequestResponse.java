package dev.hostapp.backend.dto.serverrequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.hostapp.backend.model.ServerRequest;

import java.time.Instant;
import java.util.UUID;

public record ServerRequestResponse(
        UUID id,
        @JsonProperty("owner_id") UUID ownerId,
        @JsonProperty("cpu_cores") Integer cpuCores,
        @JsonProperty("ram_gb") Integer ramGb,
        @JsonProperty("disk_gb") Integer diskGb,
        String os,
        ServerRequest.RequestStatus status,
        @JsonProperty("server_id") UUID serverId,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {
    public static ServerRequestResponse from(ServerRequest r) {
        return from(r, null);
    }

    public static ServerRequestResponse from(ServerRequest r, UUID serverId) {
        return new ServerRequestResponse(
                r.getId(),
                r.getOwner().getId(),
                r.getCpuCores(),
                r.getRamGb(),
                r.getDiskGb(),
                r.getOs(),
                r.getStatus(),
                serverId,
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
