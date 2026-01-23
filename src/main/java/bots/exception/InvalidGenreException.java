package bots.exception;

/**
 * Thrown when genre input validation fails.
 */
public class InvalidGenreException extends Exception {

    public InvalidGenreException(final String message) {
        super(message);
    }

    public InvalidGenreException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
