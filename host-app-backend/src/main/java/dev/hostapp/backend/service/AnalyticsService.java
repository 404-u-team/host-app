package dev.hostapp.backend.service;

import dev.hostapp.backend.dto.api.AnalyticsDtos.*;
import dev.hostapp.backend.domain.User;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {
    private static final String VISIBLE = " where (:admin = true or r.owner.id = :userId)";
    private final EntityManager entities;

    public AnalyticsService(EntityManager entities) { this.entities = entities; }

    @Transactional(readOnly = true)
    public Summary summary(User user) {
        Object[] counts = entities.createQuery("""
                select count(r), sum(case when r.archived = true then 1 else 0 end),
                sum(case when r.urgent = true then 1 else 0 end), avg(r.cpuCores)
                from ServerRequest r
                """ + VISIBLE, Object[].class)
                .setParameter("admin", user.isAdmin()).setParameter("userId", user.getId()).getSingleResult();
        long total = count(counts[0]);
        long archived = count(counts[1]);
        return new Summary(total, total - archived, archived, count(counts[2]), (Double) counts[3],
                groups("ref.code", "left join r.reference ref", user),
                groups("r.status", "", user), groups("r.priority", "", user));
    }

    private List<Bucket> groups(String field, String join, User user) {
        // field and join come exclusively from the fixed expressions in summary().
        String jpql = "select " + field + ", count(r) from ServerRequest r " + join + VISIBLE
                + " group by " + field + " order by " + field;
        return entities.createQuery(jpql, Object[].class).setParameter("admin", user.isAdmin())
                .setParameter("userId", user.getId()).getResultList().stream()
                .map(row -> new Bucket(row[0] == null ? "UNASSIGNED" : row[0].toString(), count(row[1]))).toList();
    }

    private long count(Object value) { return value == null ? 0 : ((Number) value).longValue(); }
}
