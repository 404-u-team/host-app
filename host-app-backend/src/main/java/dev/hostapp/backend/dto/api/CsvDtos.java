package dev.hostapp.backend.dto.api;

import java.util.List;
import java.util.UUID;

public final class CsvDtos {
    private CsvDtos() {}

    public enum Mode { CREATE_ONLY, UPSERT }
    public record RowError(long row, String message) {}
    public record Preview(UUID previewId, int validRows, int invalidRows, List<RowError> errors) {}
    public record Applied(int created, int updated) {}
}
