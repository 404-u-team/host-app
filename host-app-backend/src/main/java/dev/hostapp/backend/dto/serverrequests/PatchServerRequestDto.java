package dev.hostapp.backend.dto.serverrequests;

import com.fasterxml.jackson.annotation.JsonProperty;

import dev.hostapp.backend.model.ServerRequest.OSType;
import dev.hostapp.backend.model.ServerRequest.RequestStatus;
import jakarta.validation.constraints.Positive;


public record PatchServerRequestDto(
    @JsonProperty("cpu_cores")
    @Positive(message = "cpu_cores must be greater than 0")
    Integer cpuCores,
    @JsonProperty("ram_gb")
    @Positive(message = "ram_gb must be greater than 0")
    Integer ramGb,
    @JsonProperty("disk_gb")
    @Positive(message = "disk_gb must be greater than 0")
    Integer diskGb,
    RequestStatus status,
    OSType os
) {}
