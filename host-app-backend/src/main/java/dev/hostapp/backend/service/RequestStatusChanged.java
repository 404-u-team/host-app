package dev.hostapp.backend.service;

import dev.hostapp.backend.model.ServerRequest.RequestStatus;
import java.util.UUID;

public record RequestStatusChanged(UUID requestId, String email, boolean emailVerified,
                                   RequestStatus oldStatus, RequestStatus newStatus) {}
