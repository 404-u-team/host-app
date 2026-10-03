package dev.hostapp.cli.dto.server;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ServerData(
        String hostname,
        @JsonProperty("ipv4_addresses") List<String> ipv4Addresses,
        @JsonProperty("ipv6_addresses") List<String> ipv6Addresses,
        @JsonProperty("cpu_cores") int cpuCores,
        @JsonProperty("ram_gb") int ramGb,
        @JsonProperty("disk_gb") int diskGb
) {}
