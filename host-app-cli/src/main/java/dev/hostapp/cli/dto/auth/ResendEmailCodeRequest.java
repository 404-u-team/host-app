package dev.hostapp.cli.dto.auth;

public record ResendEmailCodeRequest(String email, String password) {}
