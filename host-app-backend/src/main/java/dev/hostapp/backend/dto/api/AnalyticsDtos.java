package dev.hostapp.backend.dto.api;

import java.time.LocalDate;
import java.util.List;

public final class AnalyticsDtos {
    private AnalyticsDtos() {}

    public record Bucket(String key, long count) {}
    public record Summary(long total, long active, long archived, long urgent, Double averageCpu,
                          List<Bucket> byReference, List<Bucket> byStatus, List<Bucket> byPriority) {}

    public enum Source { REAL, DEMO }
    public enum ForecastStatus { OK, INSUFFICIENT_DATA }
    public record DayCount(LocalDate date, int requests) {}
    public record Forecast(ForecastStatus status, Source source, LocalDate date, Integer expectedRequests,
                           String timezone, int windowDays, int historyDays, Double modelMae, Double baselineMae,
                           List<DayCount> recentHistory) {}
    public record DemoSeed(LocalDate from, LocalDate through, int days, int inserted) {}
}
