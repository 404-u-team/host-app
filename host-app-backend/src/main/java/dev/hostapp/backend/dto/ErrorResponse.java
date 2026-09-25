package dev.hostapp.backend.dto;

public record ErrorResponse(
    int status,
    String message
) {}
