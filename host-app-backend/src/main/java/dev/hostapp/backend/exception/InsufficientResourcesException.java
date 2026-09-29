package dev.hostapp.backend.exception;

public class InsufficientResourcesException extends RuntimeException {
    public InsufficientResourcesException(String message) {
        super(message);
    }
}