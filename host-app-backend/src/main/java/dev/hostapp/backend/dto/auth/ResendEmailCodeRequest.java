package dev.hostapp.backend.dto.auth;

public record ResendEmailCodeRequest(String email, String password) {}
