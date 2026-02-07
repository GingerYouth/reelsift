package parser;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retries {@link IoOperation} instances with exponential backoff.
 * The delay between attempts starts at {@code initialDelayMs} and doubles
 * after each failure. Retrying stops when total elapsed time exceeds
 * {@code maxTotalTimeMs}.
 */
public final class Retrier {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(Retrier.class);

  private final long maxTotalTimeMs;
  private final long initialDelayMs;

  /**
   * Primary constructor.
   *
   * @param maxTotalTimeMs maximum total time for all retries in milliseconds
   * @param initialDelayMs initial delay between retries in milliseconds,
   *     doubled after each failed attempt
   */
  public Retrier(final long maxTotalTimeMs, final long initialDelayMs) {
    this.maxTotalTimeMs = maxTotalTimeMs;
    this.initialDelayMs = initialDelayMs;
  }

  /**
   * Executes the given operation, retrying on {@link IOException}
   * with exponential backoff until the total retry budget is exhausted.
   * {@link NonRetryableIoException} is never retried — its original
   * exception is unwrapped and rethrown immediately.
   *
   * @param operation the IO operation to execute
   * @param <T> the result type
   * @return the result of the operation
   * @throws IOException if all retries are exhausted or the thread
   *     is interrupted during a retry delay
   */
  @SuppressWarnings("PMD.PreserveStackTrace")
  public <T> T execute(final IoOperation<T> operation) throws IOException {
    final long startTime = System.currentTimeMillis();
    long delay = this.initialDelayMs;
    IOException lastException;
    int attempt = 0;
    do {
      try {
        attempt++;
        return operation.execute();
      } catch (final NonRetryableIoException nonRetryable) {
        throw nonRetryable.unwrap();
      } catch (final IOException ex) {
        lastException = ex;
        final long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed + delay > this.maxTotalTimeMs) {
          logBudgetExhausted(attempt, elapsed);
          break;
        }
        logRetryAttempt(attempt, delay, ex);
        sleepBeforeRetry(delay, attempt, lastException);
        delay *= 2;
      }
    } while (System.currentTimeMillis() - startTime < this.maxTotalTimeMs);
    throw lastException;
  }

  /**
   * Logs a warning that the retry budget has been exhausted.
   *
   * @param attempt the number of attempts made
   * @param elapsed the total elapsed time in milliseconds
   */
  private void logBudgetExhausted(final int attempt, final long elapsed) {
    if (LOGGER.isWarnEnabled()) {
      LOGGER.warn(
          "Retry budget of {}ms exhausted after {} attempts and {}ms",
          this.maxTotalTimeMs,
          attempt,
          elapsed
      );
    }
  }

  /**
   * Logs a warning about a failed attempt before retrying.
   *
   * @param attempt the current attempt number
   * @param delay the delay before the next retry in milliseconds
   * @param failure the exception that caused the failure
   */
  private static void logRetryAttempt(
      final int attempt, final long delay, final IOException failure
  ) {
    if (LOGGER.isWarnEnabled()) {
      LOGGER.warn(
          "Attempt {} failed, retrying in {}ms: {}",
          attempt, delay, failure.getMessage()
      );
    }
  }

  /**
   * Sleeps for the given delay, preserving interrupt status and
   * wrapping the cause chain on interruption.
   *
   * @param delay the delay in milliseconds
   * @param attempt the current attempt number
   * @param cause the last IO exception before this sleep
   * @throws IOException if the thread is interrupted during sleep
   */
  private static void sleepBeforeRetry(
      final long delay, final int attempt, final IOException cause
  ) throws IOException {
    try {
      Thread.sleep(delay);
    } catch (final InterruptedException ie) {
      Thread.currentThread().interrupt();
      final IOException wrapped = new IOException(
          String.format(
              "Retry interrupted after %d attempts", attempt
          ),
          ie
      );
      wrapped.addSuppressed(cause);
      throw wrapped;
    }
  }
}
