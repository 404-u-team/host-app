package dev.hostapp.backend.service;

import dev.hostapp.backend.domain.RequestNote;
import dev.hostapp.backend.dto.api.NoteDtos.*;
import dev.hostapp.backend.exceptions.ForbiddenException;
import dev.hostapp.backend.exceptions.ResourceNotFoundException;
import dev.hostapp.backend.domain.ServerRequest;
import dev.hostapp.backend.domain.User;
import dev.hostapp.backend.repository.RequestNoteRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteService {
    private final RequestCatalogService requests;
    private final RequestNoteRepository notes;
    private final AuditService audit;

    public NoteService(RequestCatalogService requests, RequestNoteRepository notes, AuditService audit) {
        this.requests = requests;
        this.notes = notes;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<View> list(User actor, UUID id) {
        requests.findVisible(actor, id);
        return notes.findAllByRequest_IdOrderByPinnedDescCreatedAtDescId(id).stream().map(View::from).toList();
    }

    @Transactional
    public View create(User actor, UUID id, Input input) {
        ServerRequest request = active(actor, id);
        RequestNote note = notes.save(new RequestNote(request, actor, input.body().trim(), input.pinned()));
        audit.record(actor, "NOTE_CREATED", "NOTE", note.getId(), "К заявке добавлена заметка");
        return View.from(note);
    }

    @Transactional
    public View update(User actor, UUID id, UUID noteId, Input input) {
        RequestNote note = editable(actor, id, noteId);
        note.update(input.body().trim(), input.pinned());
        audit.record(actor, "NOTE_UPDATED", "NOTE", noteId, "Заметка изменена");
        return View.from(note);
    }

    @Transactional
    public void delete(User actor, UUID id, UUID noteId) {
        notes.delete(editable(actor, id, noteId));
        audit.record(actor, "NOTE_DELETED", "NOTE", noteId, "Заметка удалена");
    }

    private RequestNote editable(User actor, UUID id, UUID noteId) {
        active(actor, id);
        RequestNote note = notes.findByRequest_IdAndId(id, noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Заметка не найдена"));
        if (!actor.isAdmin() && !actor.getId().equals(note.getAuthorId())) {
            throw new ForbiddenException("Редактировать заметку может автор или администратор");
        }
        return note;
    }

    private ServerRequest active(User actor, UUID id) {
        ServerRequest request = requests.locked(actor, id);
        if (request.isArchived()) throw new IllegalStateException("Заявка в архиве");
        return request;
    }
}
