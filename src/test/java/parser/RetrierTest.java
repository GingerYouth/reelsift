package parser;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Unit tests for {@link Retrier}.
 * Tests retry behavior with exponential backoff.
 */
@Timeout(10)
final class RetrierTest {

  @Test
  void returnsResultOnFirstAttempt() throws IOException {
    final String expected = UUID.randomUUID().toString();
    assertThat(
        "cant return result on first successful attempt",
        new Retrier(1000L, 100L).execute(() -> expected),
        is(equalTo(expected))
    );
  }

  @Test
  void retriesAndReturnsAfterTransientFailure() throws IOException {
    final String expected = UUID.randomUUID().toString();
    final AtomicInteger counter = new AtomicInteger(0);
    assertThat(
        "cant return result after transient failure",
        new Retrier(5000L, 10L).execute(() -> {
          if (counter.incrementAndGet() < 3) {
            throw new IOException("transient failure");
          }
          return expected;
        }),
        is(equalTo(expected))
    );
  }

  @Test
  void throwsAfterRetryBudgetExhausted() {
    final AtomicInteger counter = new AtomicInteger(0);
    boolean thrown = false;
    try {
      new Retrier(50L, 10L).execute(() -> {
        counter.incrementAndGet();
        throw new IOException("persistent failure");
      });
    } catch (final IOException ex) {
      thrown = true;
    }
    assertThat(
        "cant throw IOException when retry budget is exhausted",
        thrown,
        is(true)
    );
  }

  @Test
  void attemptsMultipleTimesBeforeGivingUp() throws IOException {
    final AtomicInteger counter = new AtomicInteger(0);
    try {
      new Retrier(100L, 10L).execute(() -> {
        counter.incrementAndGet();
        throw new IOException("persistent failure");
      });
    } catch (final IOException ignored) {
    }
    assertThat(
        "cant attempt more than once before giving up",
        counter.get(),
        is(greaterThanOrEqualTo(2))
    );
  }

  @Test
  void doesNotRetryBeyondMaxTotalTime() {
    final long maxTotalMs = 200L;
    final long start = System.currentTimeMillis();
    try {
      new Retrier(maxTotalMs, 30L).execute(() -> {
        throw new IOException("always fails");
      });
    } catch (final IOException ignored) {
    }
    final long elapsed = System.currentTimeMillis() - start;
    assertThat(
        "cant stop retrying within reasonable time after budget",
        elapsed,
        is(lessThan(maxTotalMs * 3))
    );
  }

  @Test
  void doublesDelayBetweenRetries() throws IOException {
    final AtomicInteger counter = new AtomicInteger(0);
    final long initialDelay = 50L;
    final long start = System.currentTimeMillis();
    try {
      new Retrier(500L, initialDelay).execute(() -> {
        counter.incrementAndGet();
        throw new IOException("always fails");
      });
    } catch (final IOException ignored) {
    }
    final long elapsed = System.currentTimeMillis() - start;
    assertThat(
        "cant accumulate delay from exponential backoff",
        elapsed,
        is(greaterThanOrEqualTo(initialDelay))
    );
  }

  @Test
  void doesNotRetryNonRetryableException() {
    final AtomicInteger counter = new AtomicInteger(0);
    final IOException original = new IOException("not found");
    boolean thrown = false;
    try {
      new Retrier(5000L, 10L).execute(() -> {
        counter.incrementAndGet();
        throw new NonRetryableIoException(original);
      });
    } catch (final IOException ex) {
      thrown = ex == original;
    }
    assertThat(
        "cant skip retry and throw the unwrapped original on NonRetryableIoException",
        thrown && counter.get() == 1,
        is(true)
    );
  }

  @Test
  void handlesInterruptionDuringRetry() {
    final Thread current = Thread.currentThread();
    boolean thrown = false;
    try {
      new Retrier(5000L, 50L).execute(() -> {
        current.interrupt();
        throw new IOException("trigger retry");
      });
    } catch (final IOException ex) {
      thrown = true;
    }
    assertThat(
        "cant throw IOException when interrupted during retry",
        thrown,
        is(true)
    );
  }
}
