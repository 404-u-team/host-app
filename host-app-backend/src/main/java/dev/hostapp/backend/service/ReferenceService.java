package dev.hostapp.backend.service;

import dev.hostapp.backend.domain.OsReference;
import dev.hostapp.backend.dto.api.ReferenceDtos.*;
import dev.hostapp.backend.exceptions.ResourceNotFoundException;
import dev.hostapp.backend.domain.User;
import dev.hostapp.backend.repository.OsReferenceRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReferenceService {
    private final OsReferenceRepository references;
    private final AuditService audit;

    public ReferenceService(OsReferenceRepository references, AuditService audit) {
        this.references = references;
        this.audit = audit;
    }

    public List<View> list(boolean includeDisabled) {
        return references.findAll(Sort.by("name", "id")).stream()
                .filter(reference -> includeDisabled || reference.isEnabled()).map(View::from).toList();
    }

    @Transactional
    public View create(User actor, Create input) {
        OsReference reference = references.save(new OsReference(input.code().toUpperCase(Locale.ROOT),
                input.name().trim(), input.enabled()));
        audit.record(actor, "REFERENCE_CREATED", "OS_REFERENCE", reference.getId(), "ОС добавлена в справочник");
        return View.from(reference);
    }

    @Transactional
    public View update(User actor, UUID id, Update input) {
        OsReference reference = get(id);
        reference.update(input.name().trim(), input.enabled());
        audit.record(actor, "REFERENCE_UPDATED", "OS_REFERENCE", id, "Справочник ОС изменён");
        return View.from(reference);
    }

    public OsReference get(UUID id) {
        return references.findById(id).orElseThrow(() -> new ResourceNotFoundException("ОС не найдена"));
    }

    public OsReference forLegacy(String name) {
        return references.findFirstByNameIgnoreCaseAndEnabledTrueOrderById(name.trim())
                .orElseGet(() -> references.findByCode("OTHER").orElse(null));
    }
}
