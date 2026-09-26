package dev.hostapp.cli.dto.auth;

public record RegisterRequest(
    String name,
    String surname,
    String email,
    String password
) {
}