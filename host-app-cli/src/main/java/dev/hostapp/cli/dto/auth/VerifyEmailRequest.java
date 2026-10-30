package dev.hostapp.cli.dto.auth;

public record VerifyEmailRequest(String email, String code) {}
