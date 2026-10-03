package dev.hostapp.backend.dto.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateServerRequest(
        @NotBlank(message = "hostname is required")
        String hostname,

        @JsonProperty("ipv4_addresses")
        List<String> ipv4Addresses,

        @JsonProperty("ipv6_addresses")
        List<String> ipv6Addresses,

        @NotNull(message = "cpu_cores is required")
        @Min(value = 1, message = "cpu_cores must be >= 1")
        @JsonProperty("cpu_cores")
        Integer cpuCores,

        @NotNull(message = "ram_gb is required")
        @Min(value = 1, message = "ram_gb must be >= 1")
        @JsonProperty("ram_gb")
        Integer ramGb,

        @NotNull(message = "disk_gb is required")
        @Min(value = 1, message = "disk_gb must be >= 1")
        @JsonProperty("disk_gb")
        Integer diskGb
) {}
