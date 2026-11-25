package dev.hostapp.backend.error;

import dev.hostapp.backend.filter.CorrelationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.slf4j.MDC;
import tools.jackson.databind.json.JsonMapper;

public record ApiError(int status, String code, String message, Map<String, String> fields, String correlationId) {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    public static ApiError of(int status, String code, String message, Map<String, String> fields) {
        return new ApiError(status, code, message, fields, MDC.get("correlationId"));
    }

    public static void write(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(JSON.writeValueAsString(new ApiError(status, code, message, Map.of(),
                response.getHeader(CorrelationFilter.HEADER))));
    }
}
