package dev.hostapp.backend.dto.serverrequest;

public record StatisticsResponse(
        long total,
        long created,
        long approved,
        long rejected,
        long completed,
        long cancelled
) {}
