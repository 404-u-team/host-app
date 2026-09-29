package dev.hostapp.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateServerRequest {

    @NotNull(message = "cpu_cores is required")
    @Min(value = 1, message = "cpu_cores must be >= 1")
    @JsonProperty("cpu_cores")
    public Integer cpuCores;

    @NotNull(message = "ram_gb is required")
    @Min(value = 1, message = "ram_gb must be >= 1")
    @JsonProperty("ram_gb")
    public Integer ramGb;

    @NotNull(message = "disk_gb is required")
    @Min(value = 1, message = "disk_gb must be >= 1")
    @JsonProperty("disk_gb")
    public Integer diskGb;

    @NotBlank(message = "os is required")
    public String os;
}