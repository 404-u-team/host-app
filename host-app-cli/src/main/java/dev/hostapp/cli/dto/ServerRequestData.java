package dev.hostapp.cli.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ServerRequestData(
        @JsonProperty("cpu_cores") int cpuCores,
        @JsonProperty("ram_gb") int ramGb,
        @JsonProperty("disk_gb") int diskGb,
        String os
) {}
