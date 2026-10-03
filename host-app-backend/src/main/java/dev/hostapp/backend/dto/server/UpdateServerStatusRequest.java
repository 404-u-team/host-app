package dev.hostapp.backend.dto.server;

import dev.hostapp.backend.model.Server;
import jakarta.validation.constraints.NotNull;

public record UpdateServerStatusRequest(
        @NotNull(message = "status is required")
        Server.ServerStatus status
) {}
