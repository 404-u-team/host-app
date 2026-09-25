package dev.hostapp.cli.model;

public record LoginRequest(
        String email,
        String password
) {}
