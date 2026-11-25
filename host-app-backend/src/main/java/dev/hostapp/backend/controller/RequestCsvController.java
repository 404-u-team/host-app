package dev.hostapp.backend.controller;

import dev.hostapp.backend.dto.api.CsvDtos.*;
import dev.hostapp.backend.filter.RequestFilter;
import dev.hostapp.backend.domain.User;
import dev.hostapp.backend.service.RequestCsvService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/requests")
@SecurityRequirement(name = "bearer")
public class RequestCsvController {
    private final RequestCsvService csv;

    public RequestCsvController(RequestCsvService csv) { this.csv = csv; }

    @GetMapping(value = "/export", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> export(@RequestAttribute("user") User user,
                                         @Valid @ParameterObject @ModelAttribute RequestFilter filter) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=server-requests.csv")
                .body(csv.export(user, filter));
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Preview preview(@RequestAttribute("user") User user, @RequestParam MultipartFile file,
                           @RequestParam(defaultValue = "CREATE_ONLY") Mode mode) throws IOException {
        return csv.preview(user, file.getBytes(), mode);
    }

    @PostMapping(value = "/import/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Applied apply(@RequestAttribute("user") User user, @RequestParam MultipartFile file,
                         @RequestParam UUID previewId) throws IOException {
        return csv.apply(user, previewId, file.getBytes());
    }
}
