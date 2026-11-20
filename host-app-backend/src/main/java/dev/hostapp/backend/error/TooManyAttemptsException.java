package dev.hostapp.backend.error;

public class TooManyAttemptsException extends RuntimeException {
    public TooManyAttemptsException() {
        super("Слишком много попыток входа. Попробуйте снова через 15 минут");
    }
}
