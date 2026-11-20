package dev.hostapp.backend.repository;

import dev.hostapp.backend.domain.AuditEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface AuditRepository extends JpaRepository<AuditEvent, UUID>, JpaSpecificationExecutor<AuditEvent> {
    @Query("""
        select e.happenedAt from AuditEvent e
        where e.eventType = 'SERVER_CREATE_REQUEST' and e.happenedAt >= :from and e.happenedAt < :until
        """)
    List<Instant> requestCreations(Instant from, Instant until);
}
