package dev.hostapp.backend.dto.ServerRequestDTO;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateServerRequest{
    @JsonProperty("cpu_cores")
    public Integer cpuCores;

    @JsonProperty("ram_gb")
    public Integer ramGb;

    @JsonProperty("disk_gb")
    public Integer diskGb;

    public String os;
}
