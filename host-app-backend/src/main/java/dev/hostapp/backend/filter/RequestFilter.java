package dev.hostapp.backend.filter;

import dev.hostapp.backend.dto.api.RequestDtos;
import dev.hostapp.backend.domain.ServerRequest;
import dev.hostapp.backend.domain.ServerRequest.Priority;
import dev.hostapp.backend.domain.ServerRequest.RequestStatus;
import dev.hostapp.backend.domain.User;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

public record RequestFilter(@Size(max = 200) String q, UUID referenceId, RequestStatus status, Boolean urgent,
                            Priority priority, @Pattern(regexp = RequestDtos.TAG) String tag,
                            Archive archive, @Min(1) Integer cpu, Comparison cpuOp,
                            String sort, String direction) {
    public enum Archive { ACTIVE, ARCHIVED, ALL }
    public enum Comparison { EQ, GT, GTE, LT, LTE }

    public Specification<ServerRequest> specification(User user) {
        if (cpu == null && cpuOp != null) {
            throw new IllegalArgumentException("Укажите cpu вместе с cpuOp");
        }
        return (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (!user.isAdmin()) {
                conditions.add(cb.equal(root.get("owner").get("id"), user.getId()));
            }
            if (archive != Archive.ALL) {
                conditions.add(cb.equal(root.get("archived"), archive == Archive.ARCHIVED));
            }
            if (q != null && !q.isBlank()) {
                String pattern = ApiQuery.contains(q);
                conditions.add(cb.or(
                        cb.like(cb.lower(root.get("externalId")), pattern, '!'),
                        cb.like(cb.lower(root.get("title")), pattern, '!'),
                        cb.like(cb.lower(root.get("description")), pattern, '!'),
                        cb.like(cb.lower(root.get("os")), pattern, '!')));
            }
            if (referenceId != null) conditions.add(cb.equal(root.get("reference").get("id"), referenceId));
            if (status != null) conditions.add(cb.equal(root.get("status"), status));
            if (urgent != null) conditions.add(cb.equal(root.get("urgent"), urgent));
            if (priority != null) conditions.add(cb.equal(root.get("priority"), priority));
            if (tag != null) conditions.add(cb.isMember(tag.toLowerCase(Locale.ROOT), root.<java.util.Set<String>>get("tags")));
            if (cpu != null) {
                var cores = root.<Integer>get("cpuCores");
                Comparison operation = cpuOp == null ? Comparison.GTE : cpuOp;
                conditions.add(switch (operation) {
                    case EQ -> cb.equal(cores, cpu);
                    case GT -> cb.gt(cores, cpu);
                    case GTE -> cb.ge(cores, cpu);
                    case LT -> cb.lt(cores, cpu);
                    case LTE -> cb.le(cores, cpu);
                });
            }
            return cb.and(conditions.toArray(Predicate[]::new));
        };
    }

    public Sort ordering() {
        return ApiQuery.sort(sort == null ? "createdAt" : sort, direction == null ? "desc" : direction,
                Map.of("createdAt", "createdAt", "updatedAt", "updatedAt", "cpu", "cpuCores",
                        "ram", "ramGb", "title", "title", "externalId", "externalId", "priority", "priority"));
    }
}
