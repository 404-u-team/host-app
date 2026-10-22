package dev.hostapp.desktop.dto.auth;

import java.util.UUID;

public record LoginResponse(
    UUID id,
    String name,
    String surname,
    String email
) {
}
