package dev.hostapp.cli.dto.auth;

public record LoginRequest(
        String email,
        String password
) {}
