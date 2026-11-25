package dev.hostapp.backend.dto.api;

import java.util.List;

public final class AnalyticsDtos {
    private AnalyticsDtos() {}

    public record Bucket(String key, long count) {}
    public record Summary(long total, long active, long archived, long urgent, Double averageCpu,
                          List<Bucket> byReference, List<Bucket> byStatus, List<Bucket> byPriority) {}
}
