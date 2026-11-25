package dev.hostapp.desktop.dto.auth;

public record LoginRequest(
        String email,
        String password
) {}
