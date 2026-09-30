package dev.hostapp.backend.dto.serverrequests;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import dev.hostapp.backend.model.ServerRequest;
import dev.hostapp.backend.model.ServerRequest.OSType;
import dev.hostapp.backend.model.ServerRequest.RequestStatus;


public record ServerRequestResponse(
    UUID id,
    @JsonProperty("owner_id")
    UUID ownerId,
    @JsonProperty("cpu_cores")
    Integer cpuCores,
    @JsonProperty("ram_gb")
    Integer ramGb,
    @JsonProperty("disk_gb")
    Integer diskGb,
    OSType os,
    RequestStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    public static ServerRequestResponse from(ServerRequest request) {
        return new ServerRequestResponse(
                request.getId(),
                request.getOwner().getId(),
                request.getCpuCores(),
                request.getRamGb(),
                request.getDiskGb(),
                request.getOs(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}
