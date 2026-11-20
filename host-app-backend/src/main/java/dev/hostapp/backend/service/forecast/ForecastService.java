package dev.hostapp.backend.service.forecast;

import dev.hostapp.backend.dto.api.AnalyticsDtos.*;
import dev.hostapp.backend.exceptions.ResourceNotFoundException;
import dev.hostapp.backend.domain.User;
import dev.hostapp.backend.repository.AuditRepository;
import dev.hostapp.backend.service.AuditService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ForecastService {
    private static final int DEMO_DAYS = 120;
    private static final int MAX_DAYS = 180;
    private static final int MIN_DAYS = 35;

    private final AuditRepository events;
    private final JdbcTemplate jdbc;
    private final AuditService audit;
    private final boolean demoEnabled;
    private final ZoneId zone;

    public ForecastService(AuditRepository events, JdbcTemplate jdbc, AuditService audit,
                           @Value("${app.forecast.demo-enabled}") boolean demoEnabled,
                           @Value("${app.forecast.timezone}") String timezone) {
        this.events = events;
        this.jdbc = jdbc;
        this.audit = audit;
        this.demoEnabled = demoEnabled;
        this.zone = ZoneId.of(timezone);
    }

    @Transactional
    public DemoSeed seedDemo(User actor) {
        requireDemo();
        LocalDate today = LocalDate.now(zone);
        LocalDate first = today.minusDays(DEMO_DAYS);
        int inserted = 0;
        for (LocalDate day = first; day.isBefore(today); day = day.plusDays(1)) {
            int weekday = day.getDayOfWeek().getValue();
            int base = switch (weekday) {
                case 1, 2 -> 15;
                case 3, 4 -> 11;
                case 5 -> 7;
                default -> 2;
            };
            int count = base + Math.floorMod(day.toEpochDay() * 17, 5) - 2;
            inserted += jdbc.update("insert into forecast_demo_days(day, request_count) values (?, ?) on conflict do nothing",
                    day, count);
        }
        audit.record(actor, "FORECAST_DEMO_SEEDED", "FORECAST", null, "Подготовлена учебная история для прогноза");
        return new DemoSeed(first, today.minusDays(1), DEMO_DAYS, inserted);
    }

    @Transactional(readOnly = true)
    public Forecast forecast(Source source) {
        LocalDate today = LocalDate.now(zone);
        Map<LocalDate, Integer> counts = source == Source.DEMO ? demoCounts(today) : realCounts(today);
        LocalDate first = counts.isEmpty() ? today : counts.keySet().iterator().next();
        List<DayCount> history = first.datesUntil(today)
                .map(day -> new DayCount(day, counts.getOrDefault(day, 0))).toList();
        int length = history.size();
        List<DayCount> recent = history.subList(Math.max(0, length - RequestRnn.WINDOW), length);
        if (length < MIN_DAYS || counts.values().stream().noneMatch(count -> count > 0)) {
            return new Forecast(ForecastStatus.INSUFFICIENT_DATA, source, today.plusDays(1), null, zone.getId(),
                    RequestRnn.WINDOW, length, null, null, recent);
        }
        double[] days = history.stream().mapToDouble(DayCount::requests).toArray();
        int trainEnd = length * 4 / 5;
        RequestRnn model = RequestRnn.train(days, trainEnd);
        double modelError = 0;
        double baselineError = 0;
        for (int end = trainEnd; end + 1 < length; end++) {
            double actual = days[end + 1];
            modelError += Math.abs(model.twoDaysAhead(days, end) - actual);
            double baseline = Arrays.stream(days, end - 7, end).average().orElseThrow();
            baselineError += Math.abs(baseline - actual);
        }
        int evaluated = length - trainEnd - 1;
        int expected = (int) Math.min(Integer.MAX_VALUE, Math.round(model.twoDaysAhead(days, length)));
        return new Forecast(ForecastStatus.OK, source, today.plusDays(1), expected, zone.getId(),
                RequestRnn.WINDOW, length, modelError / evaluated, baselineError / evaluated, recent);
    }

    private Map<LocalDate, Integer> demoCounts(LocalDate today) {
        requireDemo();
        Map<LocalDate, Integer> counts = new TreeMap<>();
        jdbc.query("select day, request_count from forecast_demo_days where day >= ? and day < ?",
                row -> { counts.put(row.getObject(1, LocalDate.class), row.getInt(2)); },
                today.minusDays(DEMO_DAYS), today);
        return counts;
    }

    private Map<LocalDate, Integer> realCounts(LocalDate today) {
        Map<LocalDate, Integer> counts = new TreeMap<>();
        var from = today.minusDays(MAX_DAYS).atStartOfDay(zone).toInstant();
        var until = today.atStartOfDay(zone).toInstant();
        events.requestCreations(from, until).forEach(instant ->
                counts.merge(instant.atZone(zone).toLocalDate(), 1, Integer::sum));
        return counts;
    }

    private void requireDemo() {
        if (!demoEnabled) throw new ResourceNotFoundException("Учебный режим прогноза отключён");
    }
}
