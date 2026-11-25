package dev.hostapp.backend.service;

import dev.hostapp.backend.domain.AuditEvent;
import dev.hostapp.backend.domain.User;
import dev.hostapp.backend.repository.AuditRepository;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditRepository audit;

    public AuditService(AuditRepository audit) {
        this.audit = audit;
    }

    public void record(User actor, String event, String entity, UUID id, String description) {
        audit.save(new AuditEvent(actor == null ? null : actor.getId(), actor == null ? null : actor.getEmail(),
                event, entity, id, MDC.get("correlationId"), description));
    }
}
