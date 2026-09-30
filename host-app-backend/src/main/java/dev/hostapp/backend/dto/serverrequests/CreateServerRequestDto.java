package dev.hostapp.backend.dto.serverrequests;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import dev.hostapp.backend.model.ServerRequest.OSType;


public record CreateServerRequestDto(
    @JsonProperty("cpu_cores")
    @NotNull(message = "cpu_cores is required")
    @Positive(message = "cpu_cores must be greater than 0")
    Integer cpuCores,
    @JsonProperty("ram_gb")
    @NotNull(message = "ram_gb is required")
    @Positive(message = "ram_gb must be greater than 0")
    Integer ramGb,
    @JsonProperty("disk_gb")
    @NotNull(message = "disk_gb is required")
    @Positive(message = "disk_gb must be greater than 0")
    Integer diskGb,
    @NotNull(message = "os is required")
    OSType os
) {}
