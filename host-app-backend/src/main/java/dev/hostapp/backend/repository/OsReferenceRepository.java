package dev.hostapp.backend.repository;

import dev.hostapp.backend.domain.OsReference;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OsReferenceRepository extends JpaRepository<OsReference, UUID> {
    Optional<OsReference> findByCode(String code);
    Optional<OsReference> findFirstByNameIgnoreCaseAndEnabledTrueOrderById(String name);
}
