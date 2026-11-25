package dev.hostapp.backend.error;

import dev.hostapp.backend.exceptions.*;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmailDeliveryException.class)
    ResponseEntity<ApiError> emailFailure(EmailDeliveryException ex) {
        return error(503, "EMAIL_DELIVERY_FAILED", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> badInput(IllegalArgumentException ex) {
        return error(400, "BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            ServletRequestBindingException.class, ConstraintViolationException.class, HandlerMethodValidationException.class})
    ResponseEntity<ApiError> badRequest(Exception ex) {
        return error(400, "BAD_REQUEST", "Проверьте формат тела запроса и параметры");
    }

    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<ApiError> unauthorized(UnauthorizedException ex) {
        return error(401, "UNAUTHORIZED", ex.getMessage());
    }

    @ExceptionHandler({ResourceNotFoundException.class, UserNotFoundException.class})
    ResponseEntity<ApiError> notFound(RuntimeException ex) {
        return error(404, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> unknownRoute() {
        return error(404, "NOT_FOUND", "Маршрут не найден");
    }

    @ExceptionHandler(ForbiddenException.class)
    ResponseEntity<ApiError> forbidden(ForbiddenException ex) {
        return error(403, "FORBIDDEN", ex.getMessage());
    }

    @ExceptionHandler({IllegalStateException.class, UserAlreadyExistsException.class})
    ResponseEntity<ApiError> conflict(RuntimeException ex) {
        return error(409, "CONFLICT", ex.getMessage());
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, VersionConflictException.class})
    ResponseEntity<ApiError> optimisticLock() {
        return error(409, "VERSION_CONFLICT", "Запись уже изменилась. Обновите данные");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> constraint() {
        return error(409, "CONFLICT", "Данные конфликтуют с существующей записью");
    }

    @ExceptionHandler(TooManyAttemptsException.class)
    ResponseEntity<ApiError> rateLimit(TooManyAttemptsException ex) {
        return ResponseEntity.status(429).header("Retry-After", "900")
                .body(ApiError.of(429, "TOO_MANY_ATTEMPTS", ex.getMessage(), Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(field -> fields.put(field.getField(), field.getDefaultMessage()));
        return ResponseEntity.badRequest().body(ApiError.of(400, "VALIDATION_ERROR", "Проверьте поля запроса", fields));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> uploadLimit() {
        return error(400, "BAD_REQUEST", "Размер файла превышает допустимый предел");
    }

    @ExceptionHandler(ErrorResponseException.class)
    ResponseEntity<ApiError> framework(ErrorResponseException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        return error(ex.getStatusCode().value(), status == null ? "HTTP_ERROR" : status.name(), "Запрос отклонён");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex) {
        LOG.error("Необработанная ошибка запроса", ex);
        return error(500, "INTERNAL_ERROR", "Не удалось обработать запрос");
    }

    private ResponseEntity<ApiError> error(int status, String code, String message) {
        return ResponseEntity.status(status).body(ApiError.of(status, code, message, Map.of()));
    }
}
