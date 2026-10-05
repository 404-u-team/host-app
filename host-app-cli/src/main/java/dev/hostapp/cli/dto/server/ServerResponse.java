package dev.hostapp.cli.dto.server;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public record ServerResponse(
        UUID id,
        String hostname,
        String os,
        @JsonProperty("ipv4_addresses") List<String> ipv4Addresses,
        @JsonProperty("ipv6_addresses") List<String> ipv6Addresses,
        @JsonProperty("cpu_cores") Integer cpuCores,
        @JsonProperty("ram_gb") Integer ramGb,
        @JsonProperty("disk_gb") Integer diskGb,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("updated_at") String updatedAt
) {}
