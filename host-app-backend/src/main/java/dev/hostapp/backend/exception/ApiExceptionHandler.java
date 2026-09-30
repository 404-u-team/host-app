package dev.hostapp.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.hostapp.backend.dto.ErrorResponse;


@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleNoChangesProvided(
        ApiException exception
    ) {
        ErrorResponse response = new ErrorResponse(
            exception.getStatus().value(),
            exception.getMessage()
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(
        MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .distinct()
            .reduce((first, second) -> first + "; " + second)
            .orElse("Validation failed");

        ErrorResponse response = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            message
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response);
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
        IllegalArgumentException exception
    ) {
        ErrorResponse response = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            exception.getMessage()
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
        UnauthorizedException exception
    ) {
        ErrorResponse response = new ErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            exception.getMessage()
        );

        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(response);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
        UserNotFoundException exception
    ) {
        ErrorResponse response = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            exception.getMessage()
        );

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(response);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(
        UserAlreadyExistsException exception
    ) {
        ErrorResponse response = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            exception.getMessage()
        );

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(response);
    }
}
