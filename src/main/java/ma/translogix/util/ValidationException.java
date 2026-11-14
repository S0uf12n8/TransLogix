package ma.translogix.util;

/** Thrown by services when user input breaks a business rule. Controllers show its message in a dialog. */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
