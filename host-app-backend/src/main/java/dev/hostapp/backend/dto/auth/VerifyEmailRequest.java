package dev.hostapp.backend.dto.auth;

public record VerifyEmailRequest(String email, String code) {}
