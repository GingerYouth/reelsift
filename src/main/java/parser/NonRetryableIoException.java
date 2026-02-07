package parser;

import java.io.IOException;

/**
 * Signals that an {@link IOException} should not be retried by {@link Retrier}.
 * Wraps the original exception so that after the Retrier catches this type,
 * it can {@link #unwrap()} and rethrow the original cause.
 *
 * <p>Usage example:
 * <pre>
 * try {
 *   connection.execute();
 * } catch (HttpStatusException ex) {
 *   if (ex.getStatusCode() == 404) {
 *     throw new NonRetryableIoException(ex);
 *   }
 *   throw ex;
 * }
 * </pre>
 */
public final class NonRetryableIoException extends IOException {

  private static final long serialVersionUID = 1L;

  private final IOException original;

  /**
   * Primary constructor.
   *
   * @param original the original IOException that should not be retried
   */
  public NonRetryableIoException(final IOException original) {
    super(original);
    this.original = original;
  }

  /**
   * Returns the original exception that was wrapped.
   *
   * @return the original IOException
   */
  public IOException unwrap() {
    return this.original;
  }
}
