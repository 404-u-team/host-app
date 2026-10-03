package dev.hostapp.backend.dto.error;

public record ErrorResponse(
    int status,
    String message
) {}
