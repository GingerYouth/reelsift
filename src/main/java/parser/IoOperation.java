package parser;

import java.io.IOException;

/**
 * Functional interface for IO operations that may throw {@link IOException}.
 * Used with {@link Retrier} to retry transient failures.
 *
 * @param <T> the type of result produced by the operation
 */
@FunctionalInterface
public interface IoOperation<T> {

  /**
   * Executes the IO operation.
   *
   * @return the result of the operation
   * @throws IOException if the operation fails
   */
  T execute() throws IOException;
}
