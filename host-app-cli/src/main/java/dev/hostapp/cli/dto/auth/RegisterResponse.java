package dev.hostapp.cli.dto.auth;

import java.util.UUID;

public record RegisterResponse(
    UUID id,
    String name,
    String surname,
    String email
) {
}
