package dev.hostapp.backend.dto.auth;

public record LoginRequest(
    String email,
    String password
) {}
