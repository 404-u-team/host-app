package dev.hostapp.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.hostapp.backend.model.ServerRequest;

import java.time.Instant;
import java.util.UUID;

public class ServerRequestResponse {

    @JsonProperty("id")
    public UUID id;

    @JsonProperty("owner_id")
    public UUID ownerId;

    @JsonProperty("cpu_cores")
    public Integer cpuCores;

    @JsonProperty("ram_gb")
    public Integer ramGb;

    @JsonProperty("disk_gb")
    public Integer diskGb;

    public String os;

    public ServerRequest.RequestStatus status;

    @JsonProperty("server_id")
    public UUID serverId; // null, если заявка ещё не размещена на сервере

    @JsonProperty("created_at")
    public Instant createdAt;

    @JsonProperty("updated_at")
    public Instant updatedAt;

    public static ServerRequestResponse from(ServerRequest r) {
        ServerRequestResponse dto = new ServerRequestResponse();
        dto.id = r.getId();
        dto.ownerId = r.getOwner().getId();
        dto.cpuCores = r.getCpuCores();
        dto.ramGb = r.getRamGb();
        dto.diskGb = r.getDiskGb();
        dto.os = r.getOs();
        dto.status = r.getStatus();
        dto.createdAt = r.getCreatedAt();
        dto.updatedAt = r.getUpdatedAt();
        return dto;
    }

    public static ServerRequestResponse from(ServerRequest r, UUID serverId) {
        ServerRequestResponse dto = from(r);
        dto.serverId = serverId;
        return dto;
    }
}
