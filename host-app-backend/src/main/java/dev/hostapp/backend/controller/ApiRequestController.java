package dev.hostapp.backend.controller;

import dev.hostapp.backend.dto.api.RequestDtos.*;
import dev.hostapp.backend.exceptions.ForbiddenException;
import dev.hostapp.backend.exceptions.ResourceNotFoundException;
import dev.hostapp.backend.filter.RequestFilter;
import dev.hostapp.backend.domain.User;
import dev.hostapp.backend.service.RequestCatalogService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/requests")
@SecurityRequirement(name = "bearer")
public class ApiRequestController {
    private final RequestCatalogService requests;

    public ApiRequestController(RequestCatalogService requests) { this.requests = requests; }

    @GetMapping
    public Page<View> list(@RequestAttribute("user") User user, @Valid @ParameterObject @ModelAttribute RequestFilter filter,
                           @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return requests.list(user, filter, page, size);
    }

    @GetMapping("/{id}")
    public View get(@RequestAttribute("user") User user, @PathVariable UUID id) { return requests.get(user, id); }

    @PostMapping
    public ResponseEntity<View> create(@RequestAttribute("user") User user, @Valid @RequestBody Data data) {
        return ResponseEntity.status(201).body(requests.create(user, data));
    }

    @PutMapping("/{id}")
    public View update(@RequestAttribute("user") User user, @PathVariable UUID id, @Valid @RequestBody Update input) {
        return requests.update(user, id, input);
    }

    @DeleteMapping("/{id}")
    public View archive(@RequestAttribute("user") User user, @PathVariable UUID id, @RequestParam long version) {
        return requests.archive(user, id, version, true);
    }

    @PostMapping("/{id}/restore")
    public View restore(@RequestAttribute("user") User user, @PathVariable UUID id, @Valid @RequestBody Version input) {
        return requests.archive(user, id, input.version(), false);
    }

    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> delete(@RequestAttribute("user") User user, @PathVariable UUID id) {
        requests.permanentDelete(user, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk")
    public BulkResult bulk(@RequestAttribute("user") User user, @Valid @RequestBody Bulk input) {
        if (input.action() == BulkAction.SET_STATUS && input.status() == null) {
            throw new IllegalArgumentException("Для SET_STATUS укажите status");
        }
        if ((input.action() == BulkAction.ADD_TAG || input.action() == BulkAction.REMOVE_TAG) && input.tag() == null) {
            throw new IllegalArgumentException("Для операции с тегом укажите tag");
        }
        List<UUID> succeeded = new ArrayList<>();
        List<BulkError> errors = new ArrayList<>();
        for (UUID id : input.ids()) {
            try {
                requests.bulkOne(user, id, input);
                succeeded.add(id);
            } catch (ForbiddenException | ResourceNotFoundException | IllegalStateException ex) {
                errors.add(new BulkError(id, ex.getMessage()));
            } catch (OptimisticLockingFailureException | DataIntegrityViolationException ex) {
                errors.add(new BulkError(id, "Конфликт при сохранении"));
            }
        }
        return new BulkResult(succeeded, errors);
    }
}
