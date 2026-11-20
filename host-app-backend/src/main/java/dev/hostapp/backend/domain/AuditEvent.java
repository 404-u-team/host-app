package dev.hostapp.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID actorId;
    private String actorLogin;
    @Column(nullable = false, length = 64)
    private String eventType;
    @Column(nullable = false, length = 64)
    private String entityType;
    private UUID entityId;
    @Column(nullable = false)
    private Instant happenedAt;
    @Column(length = 64)
    private String correlationId;
    @Column(nullable = false, length = 500)
    private String description;

    protected AuditEvent() {}

    public AuditEvent(UUID actorId, String actorLogin, String eventType, String entityType, UUID entityId,
                      String correlationId, String description) {
        this.actorId = actorId;
        this.actorLogin = actorLogin;
        this.eventType = eventType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.happenedAt = Instant.now();
        this.correlationId = correlationId;
        this.description = description;
    }

    public UUID getId() { return id; }
    public UUID getActorId() { return actorId; }
    public String getActorLogin() { return actorLogin; }
    public String getEventType() { return eventType; }
    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public Instant getHappenedAt() { return happenedAt; }
    public String getCorrelationId() { return correlationId; }
    public String getDescription() { return description; }
}
