package dev.hostapp.cli.model;

import java.util.UUID;

public record User(
        UUID id,
        String name,
        String surname,
        String email
) {}
