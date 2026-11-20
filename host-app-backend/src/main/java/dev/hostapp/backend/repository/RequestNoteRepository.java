package dev.hostapp.backend.repository;

import dev.hostapp.backend.domain.RequestNote;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestNoteRepository extends JpaRepository<RequestNote, UUID> {
    List<RequestNote> findAllByRequest_IdOrderByPinnedDescCreatedAtDescId(UUID requestId);
    Optional<RequestNote> findByRequest_IdAndId(UUID requestId, UUID id);
}
