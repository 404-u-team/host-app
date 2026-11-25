package dev.hostapp.backend.service;

import dev.hostapp.backend.domain.OsReference;
import dev.hostapp.backend.dto.api.RequestDtos.*;
import dev.hostapp.backend.error.VersionConflictException;
import dev.hostapp.backend.exceptions.ForbiddenException;
import dev.hostapp.backend.exceptions.ResourceNotFoundException;
import dev.hostapp.backend.filter.ApiQuery;
import dev.hostapp.backend.filter.RequestFilter;
import dev.hostapp.backend.domain.ServerRequest;
import dev.hostapp.backend.domain.User;
import dev.hostapp.backend.repository.ServerRequestRepository;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequestCatalogService {
    private final ServerRequestRepository requests;
    private final ReferenceService references;
    private final ServerRequestService workflow;
    private final AuditService audit;

    public RequestCatalogService(ServerRequestRepository requests, ReferenceService references,
                                 ServerRequestService workflow, AuditService audit) {
        this.requests = requests;
        this.references = references;
        this.workflow = workflow;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public Page<View> list(User user, RequestFilter filter, int page, int size) {
        return requests.findAll(filter.specification(user), ApiQuery.page(page, size, filter.ordering())).map(View::from);
    }

    @Transactional(readOnly = true)
    public List<View> export(User user, RequestFilter filter) {
        return requests.findAll(filter.specification(user), filter.ordering()).stream().map(View::from).toList();
    }

    @Transactional(readOnly = true)
    public View get(User user, UUID id) { return View.from(findVisible(user, id)); }

    @Transactional
    public View create(User actor, Data data) {
        OsReference reference = references.get(data.referenceId());
        if (!reference.isEnabled()) {
            throw new IllegalArgumentException("ОС отключена");
        }
        ServerRequest request = new ServerRequest(actor, data.cpuCores(), data.ramGb(), data.diskGb(), reference.getName());
        applyData(request, data, reference);
        requests.saveAndFlush(request);
        audit.record(actor, "SERVER_CREATE_REQUEST", "REQUEST", request.getId(), "Создана заявка на сервер");
        return View.from(request);
    }

    @Transactional
    public View update(User actor, UUID id, Update input) {
        ServerRequest request = locked(actor, id);
        checkVersion(request, input.version());
        return updateData(actor, request, input.data());
    }

    View updateData(User actor, ServerRequest request, Data data) {
        ensureEditable(request);
        OsReference reference = references.get(data.referenceId());
        if (!reference.isEnabled() && (request.getReference() == null
                || !Objects.equals(request.getReference().getId(), reference.getId()))) {
            throw new IllegalArgumentException("ОС отключена");
        }
        request.setCpuCores(data.cpuCores());
        request.setRamGb(data.ramGb());
        request.setDiskGb(data.diskGb());
        request.setOs(reference.getName());
        applyData(request, data, reference);
        requests.flush();
        audit.record(actor, "REQUEST_UPDATED", "REQUEST", request.getId(), "Параметры заявки изменены");
        return View.from(request);
    }

    @Transactional
    public View archive(User actor, UUID id, long version, boolean archived) {
        ServerRequest request = locked(actor, id);
        checkVersion(request, version);
        setArchive(actor, request, archived);
        requests.flush();
        return View.from(request);
    }

    @Transactional
    public void permanentDelete(User actor, UUID id) {
        requireAdmin(actor);
        ServerRequest request = locked(actor, id);
        if (!request.isArchived()) {
            throw new IllegalStateException("Сначала отправьте заявку в архив");
        }
        if (request.getType() == ServerRequest.RequestType.CREATE && request.getServer() != null) {
            throw new IllegalStateException("Связанный сервер ещё существует");
        }
        requests.delete(request);
        audit.record(actor, "REQUEST_DELETED", "REQUEST", id, "Заявка удалена окончательно");
    }

    @Transactional
    public void bulkOne(User actor, UUID id, Bulk input) {
        ServerRequest request = locked(actor, id);
        switch (input.action()) {
            case ARCHIVE -> setArchive(actor, request, true);
            case RESTORE -> setArchive(actor, request, false);
            case SET_STATUS -> {
                requireAdmin(actor);
                workflow.updateStatus(id, input.status(), actor);
            }
            case ADD_TAG, REMOVE_TAG -> {
                if (request.isArchived()) throw new IllegalStateException("Заявка в архиве");
                String tag = input.tag().toLowerCase(Locale.ROOT);
                if (input.action() == BulkAction.ADD_TAG) {
                    if (request.getTags().size() >= 20 && !request.getTags().contains(tag)) {
                        throw new IllegalStateException("В заявке может быть не более 20 тегов");
                    }
                    request.addTag(tag);
                } else {
                    request.removeTag(tag);
                }
                audit.record(actor, "REQUEST_TAGS_CHANGED", "REQUEST", id, "Изменены теги заявки");
            }
        }
    }

    ServerRequest findVisible(User user, UUID id) {
        ServerRequest request = requests.findById(id).orElseThrow(RequestCatalogService::notFound);
        checkAccess(user, request);
        return request;
    }

    ServerRequest locked(User user, UUID id) {
        ServerRequest request = requests.findByIdForUpdate(id).orElseThrow(RequestCatalogService::notFound);
        checkAccess(user, request);
        return request;
    }

    static void ensureEditable(ServerRequest request) {
        if (request.isArchived() || request.getStatus() != ServerRequest.RequestStatus.CREATED
                || request.getType() == ServerRequest.RequestType.CANCEL) {
            throw new IllegalStateException("Редактировать можно активную заявку в статусе CREATED с параметрами сервера");
        }
    }

    private void applyData(ServerRequest request, Data data, OsReference reference) {
        request.setReference(reference);
        request.setDetails(data.externalId() == null ? request.getExternalId() : data.externalId().toUpperCase(Locale.ROOT),
                data.title().trim(), data.description().trim(), data.urgent(),
                data.tags().stream().map(tag -> tag.toLowerCase(Locale.ROOT)).collect(Collectors.toSet()));
    }

    private void setArchive(User actor, ServerRequest request, boolean archived) {
        if (request.isArchived() != archived) {
            request.setArchived(archived);
            audit.record(actor, archived ? "REQUEST_ARCHIVED" : "REQUEST_RESTORED", "REQUEST", request.getId(),
                    archived ? "Заявка отправлена в архив" : "Заявка восстановлена");
        }
    }

    private void checkAccess(User user, ServerRequest request) {
        if (!user.isAdmin() && !user.getId().equals(request.getOwner().getId())) {
            throw new ForbiddenException("Нет доступа к заявке");
        }
    }

    private void checkVersion(ServerRequest request, long version) {
        if (request.getVersion() != version) throw new VersionConflictException();
    }

    private void requireAdmin(User user) {
        if (!user.isAdmin()) throw new ForbiddenException("Действие доступно администратору");
    }

    private static ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("Заявка не найдена");
    }
}
