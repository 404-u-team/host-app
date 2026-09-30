package dev.hostapp.cli.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record ServerRequestResponse(
        UUID id,
        @JsonProperty("owner_id") UUID ownerId,
        @JsonProperty("cpu_cores") Integer cpuCores,
        @JsonProperty("ram_gb") Integer ramGb,
        @JsonProperty("disk_gb") Integer diskGb,
        String os,
        String status,
        @JsonProperty("server_id") UUID serverId,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("updated_at") String updatedAt
) {}
