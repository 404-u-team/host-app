package dev.hostapp.desktop.dto.auth;

public record RegisterRequest(
    String name,
    String surname,
    String email,
    String password
) {
}