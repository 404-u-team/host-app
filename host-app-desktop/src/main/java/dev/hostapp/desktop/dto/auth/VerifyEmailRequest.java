package dev.hostapp.desktop.dto.auth;

public record VerifyEmailRequest(String email, String code) {}
