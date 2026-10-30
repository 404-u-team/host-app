package dev.hostapp.backend.service;

import dev.hostapp.backend.model.ServerRequest.RequestStatus;
import dev.hostapp.backend.model.ServerRequest.RequestType;
import java.util.UUID;

public record RequestStatusChanged(UUID requestId, String email, boolean emailVerified,
                                   RequestStatus oldStatus, RequestStatus newStatus, RequestType type,
                                   Integer cpuCores, Integer ramGb, Integer diskGb, String os,
                                   UUID serverId, String hostname) {}
