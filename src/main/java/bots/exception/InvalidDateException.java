package bots.exception;

/**
 * Thrown when date input validation fails.
 */
public class InvalidDateException extends Exception {

    public InvalidDateException(final String message) {
        super(message);
    }

    public InvalidDateException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
