package dev.hostapp.backend.dto.error;

import java.util.Map;

public record ValidationErrorResponse(String code, String message, Map<String, String> errors) {}
