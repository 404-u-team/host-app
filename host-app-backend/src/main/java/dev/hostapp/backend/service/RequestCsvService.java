package dev.hostapp.backend.service;

import dev.hostapp.backend.domain.OsReference;
import dev.hostapp.backend.dto.api.CsvDtos.*;
import dev.hostapp.backend.dto.api.RequestDtos;
import dev.hostapp.backend.dto.api.RequestDtos.Data;
import dev.hostapp.backend.dto.api.RequestDtos.View;
import dev.hostapp.backend.exceptions.ResourceNotFoundException;
import dev.hostapp.backend.filter.RequestFilter;
import dev.hostapp.backend.domain.ServerRequest;
import dev.hostapp.backend.domain.User;
import dev.hostapp.backend.repository.OsReferenceRepository;
import dev.hostapp.backend.repository.ServerRequestRepository;
import dev.hostapp.backend.security.Tokens;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequestCsvService {
    private static final int MAX_ROWS = 1000;
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final List<String> INPUT_COLUMNS = List.of("externalId", "title", "description", "referenceCode",
            "cpuCores", "ramGb", "diskGb", "urgent", "tags");
    private static final List<String> OUTPUT_COLUMNS = List.of("externalId", "title", "description", "referenceCode",
            "cpuCores", "ramGb", "diskGb", "urgent", "tags", "id", "status", "type", "priority", "archived",
            "createdAt", "updatedAt", "version");
    private final RequestCatalogService catalog;
    private final ServerRequestRepository requests;
    private final OsReferenceRepository references;
    private final JdbcTemplate jdbc;
    private final Validator validator;
    private final AuditService audit;

    public RequestCsvService(RequestCatalogService catalog, ServerRequestRepository requests,
                             OsReferenceRepository references, JdbcTemplate jdbc, Validator validator, AuditService audit) {
        this.catalog = catalog;
        this.requests = requests;
        this.references = references;
        this.jdbc = jdbc;
        this.validator = validator;
        this.audit = audit;
    }

    public String export(User user, RequestFilter filter) {
        StringWriter buffer = new StringWriter();
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader(OUTPUT_COLUMNS.toArray(String[]::new)).get();
        try (CSVPrinter output = new CSVPrinter(buffer, format)) {
            for (View row : catalog.export(user, filter)) {
                output.printRecord(escape(row.externalId()), escape(row.title()), escape(row.description()),
                        escape(row.referenceCode()), row.cpuCores(), row.ramGb(), row.diskGb(), row.urgent(),
                        escape(row.tags().stream().sorted().collect(Collectors.joining("|"))), row.id(),
                        row.status(), row.type(), row.priority(), row.archived(), row.createdAt(), row.updatedAt(), row.version());
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return buffer.toString();
    }

    @Transactional
    public Preview preview(User actor, byte[] bytes, Mode mode) {
        Plan plan = plan(actor, bytes, mode, false);
        UUID ticket = null;
        if (plan.errors().isEmpty()) {
            ticket = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            jdbc.update("delete from csv_import_previews where expires_at <= ?", now);
            jdbc.update("""
                    insert into csv_import_previews (id, user_id, file_hash, plan_hash, mode, expires_at)
                    values (?, ?, ?, ?, ?, ?)
                    """, ticket, actor.getId(), Tokens.hash(bytes), planHash(plan), mode.name(), now.plusMinutes(15));
        }
        return new Preview(ticket, plan.rows().size(), plan.errors().size(), plan.errors());
    }

    @Transactional
    public Applied apply(User actor, UUID id, byte[] bytes) {
        checkSize(bytes);
        Ticket ticket = jdbc.query("""
                select file_hash, plan_hash, mode, expires_at from csv_import_previews
                where id = ? and user_id = ? for update
                """, (row, index) -> new Ticket(row.getString(1), row.getString(2), Mode.valueOf(row.getString(3)),
                row.getObject(4, OffsetDateTime.class).toInstant()), id, actor.getId()).stream()
                .findFirst().orElseThrow(() -> new ResourceNotFoundException("Проверка импорта не найдена"));
        if (!ticket.expiresAt().isAfter(Instant.now()) || !ticket.fileHash().equals(Tokens.hash(bytes))) {
            throw new IllegalStateException("Файл изменился или срок проверки истёк. Выполните preview ещё раз");
        }
        Plan plan = plan(actor, bytes, ticket.mode(), true);
        if (!plan.errors().isEmpty() || !ticket.planHash().equals(planHash(plan))) {
            throw new IllegalStateException("Данные после проверки изменились. Выполните preview ещё раз");
        }
        int created = 0;
        int updated = 0;
        for (Row row : plan.rows()) {
            if (row.existing() == null) {
                catalog.create(actor, row.data());
                created++;
            } else {
                catalog.updateData(actor, row.existing(), row.data());
                updated++;
            }
        }
        jdbc.update("delete from csv_import_previews where id = ?", id);
        audit.record(actor, "CSV_IMPORTED", "REQUEST", null, "Создано записей: " + created + ", обновлено: " + updated);
        return new Applied(created, updated);
    }

    private Plan plan(User actor, byte[] bytes, Mode mode, boolean lockRows) {
        checkSize(bytes);
        List<RowError> errors = new ArrayList<>();
        List<Row> rows = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get();
        try {
            String text = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
            if (text.startsWith("\uFEFF")) text = text.substring(1);
            try (CSVParser parser = CSVParser.builder().setReader(new StringReader(text)).setFormat(format).get()) {
                if (!parser.getHeaderNames().containsAll(INPUT_COLUMNS)) {
                    throw new IllegalArgumentException("Заголовок CSV должен содержать " + INPUT_COLUMNS);
                }
                for (CSVRecord entry : parser) {
                    long rowNumber = entry.getRecordNumber();
                    if (rowNumber > MAX_ROWS) throw new IllegalArgumentException("В CSV допустимо не более " + MAX_ROWS + " записей");
                    try {
                        if (!entry.isConsistent()) throw new IllegalArgumentException("Число полей отличается от заголовка");
                        Data data = data(entry);
                        if (!seen.add(data.externalId())) throw new IllegalArgumentException("Повтор externalId в файле");
                        rows.add(new Row(rowNumber, data, null));
                    } catch (IllegalArgumentException exception) {
                        errors.add(new RowError(rowNumber, exception.getMessage()));
                    }
                }
            }
        } catch (IOException | UncheckedIOException exception) {
            throw new IllegalArgumentException("Проверьте синтаксис CSV и кодировку UTF-8");
        }
        if (rows.isEmpty() && errors.isEmpty()) throw new IllegalArgumentException("CSV не содержит записей");
        List<Row> valid = new ArrayList<>();
        rows.sort(Comparator.comparing(row -> row.data().externalId()));
        for (Row row : rows) {
            Data data = row.data();
            try {
                ServerRequest existing = (lockRows ? requests.findByExternalIdForUpdate(data.externalId())
                        : requests.findByExternalId(data.externalId())).orElse(null);
                verifyRow(actor, mode, data, existing);
                valid.add(new Row(row.number(), data, existing));
            } catch (IllegalArgumentException | IllegalStateException exception) {
                errors.add(new RowError(row.number(), exception.getMessage()));
            }
        }
        errors.sort(Comparator.comparingLong(RowError::row));
        return new Plan(valid, errors);
    }

    private Data data(CSVRecord entry) {
        String id = unescape(entry.get("externalId")).trim().toUpperCase(Locale.ROOT);
        if (!id.matches(RequestDtos.EXTERNAL_ID)) throw new IllegalArgumentException("Некорректный externalId");
        String code = unescape(entry.get("referenceCode")).trim().toUpperCase(Locale.ROOT);
        OsReference reference = references.findByCode(code).orElseThrow(() -> new IllegalArgumentException("Неизвестный код ОС"));
        String urgent = entry.get("urgent");
        if (!urgent.equalsIgnoreCase("true") && !urgent.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException("urgent должен быть true или false");
        }
        Set<String> tags = Arrays.stream(unescape(entry.get("tags")).split("\\|", -1))
                .map(String::trim).filter(tag -> !tag.isEmpty()).collect(Collectors.toSet());
        Data data = new Data(id, unescape(entry.get("title")), unescape(entry.get("description")), reference.getId(),
                number(entry, "cpuCores"), number(entry, "ramGb"), number(entry, "diskGb"),
                Boolean.parseBoolean(urgent), tags);
        Set<ConstraintViolation<Data>> violations = validator.validate(data);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(violations.stream()
                    .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                    .sorted().collect(Collectors.joining(", ")));
        }
        return data;
    }

    private void verifyRow(User actor, Mode mode, Data data, ServerRequest existing) {
        if (existing != null) {
            if (!actor.isAdmin() && !actor.getId().equals(existing.getOwner().getId())) {
                throw new IllegalArgumentException("externalId недоступен для импорта");
            }
            if (mode == Mode.CREATE_ONLY) throw new IllegalArgumentException("externalId уже существует");
            RequestCatalogService.ensureEditable(existing);
        }
        OsReference reference = references.findById(data.referenceId()).orElseThrow();
        if (!reference.isEnabled() && (existing == null || existing.getReference() == null
                || !reference.getId().equals(existing.getReference().getId()))) {
            throw new IllegalArgumentException("ОС отключена");
        }
    }

    private String planHash(Plan plan) {
        return Tokens.hash(plan.rows().stream().map(row -> row.data().externalId() + ":" + row.data().referenceId() + ":"
                + (row.existing() == null ? "new" : row.existing().getId() + ":" + row.existing().getVersion()))
                .collect(Collectors.joining("\n")));
    }

    private int number(CSVRecord row, String field) {
        try {
            return Integer.parseInt(row.get(field).trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(field + " должен быть целым числом");
        }
    }

    private void checkSize(byte[] bytes) {
        if (bytes.length == 0 || bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("CSV должен содержать данные и занимать не более 2 МБ");
        }
    }

    private String escape(String value) {
        if (value == null) return "";
        return requiresEscape(value) ? "'" + value : value;
    }

    private String unescape(String value) {
        return value.startsWith("'") && requiresEscape(value.substring(1)) ? value.substring(1) : value;
    }

    private boolean requiresEscape(String value) {
        String trimmed = value.stripLeading();
        return !value.isEmpty() && (value.charAt(0) == '\t' || value.charAt(0) == '\r' || value.charAt(0) == '\n'
                || !trimmed.isEmpty() && "=+-@'".indexOf(trimmed.charAt(0)) >= 0);
    }

    private record Row(long number, Data data, ServerRequest existing) {}
    private record Plan(List<Row> rows, List<RowError> errors) {}
    private record Ticket(String fileHash, String planHash, Mode mode, Instant expiresAt) {}
}
