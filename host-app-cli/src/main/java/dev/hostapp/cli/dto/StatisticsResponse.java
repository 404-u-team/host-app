package dev.hostapp.cli.dto;

public record StatisticsResponse(
        long total,
        long created,
        long approved,
        long rejected,
        long completed,
        long cancelled
) {}
