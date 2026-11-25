package dev.hostapp.backend.dto.api;

import dev.hostapp.backend.domain.AuditEvent;
import java.time.Instant;
import java.util.UUID;

public record AuditView(UUID id, UUID actorId, String actorLogin, String eventType, String entityType,
                        UUID entityId, Instant happenedAt, String correlationId, String description) {
    public static AuditView from(AuditEvent event) {
        return new AuditView(event.getId(), event.getActorId(), event.getActorLogin(), event.getEventType(),
                event.getEntityType(), event.getEntityId(), event.getHappenedAt(), event.getCorrelationId(),
                event.getDescription());
    }
}
