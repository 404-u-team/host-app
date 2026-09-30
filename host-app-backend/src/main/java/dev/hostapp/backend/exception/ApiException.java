package dev.hostapp.backend.exception; 

import org.springframework.http.HttpStatus;


// Base API exception with configurable status and message
// Please use this one. Dont create millions of files and wrappers
// for them in exception handler
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}