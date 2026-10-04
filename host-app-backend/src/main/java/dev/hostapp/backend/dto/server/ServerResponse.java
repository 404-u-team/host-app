package dev.hostapp.backend.dto.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.hostapp.backend.model.Server;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ServerResponse(
        UUID id,
        String hostname,
        String os,
        @JsonProperty("ipv4_addresses") List<String> ipv4Addresses,
        @JsonProperty("ipv6_addresses") List<String> ipv6Addresses,
        Server.ServerStatus status,
        @JsonProperty("cpu_cores") Integer cpuCores,
        @JsonProperty("ram_gb") Integer ramGb,
        @JsonProperty("disk_gb") Integer diskGb,
        @JsonProperty("available_cpu_cores") Integer availableCpuCores,
        @JsonProperty("available_ram_gb") Integer availableRamGb,
        @JsonProperty("available_disk_gb") Integer availableDiskGb,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {
    public static ServerResponse from(Server server) {
        return new ServerResponse(
                server.getId(),
                server.getHostname(),
                server.getOs(),
                List.copyOf(server.getIpv4Addresses()),
                List.copyOf(server.getIpv6Addresses()),
                server.getStatus(),
                server.getCpuCores(),
                server.getRamGb(),
                server.getDiskGb(),
                server.getAvailableCpuCores(),
                server.getAvailableRamGb(),
                server.getAvailableDiskGb(),
                server.getCreatedAt(),
                server.getUpdatedAt()
        );
    }
}
