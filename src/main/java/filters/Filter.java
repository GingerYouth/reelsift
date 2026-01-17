package filters;

import parser.Session;

import java.util.stream.Stream;

/** Filter interface. */
@FunctionalInterface
public interface Filter {
    Stream<Session> filter(Stream<Session> shows);
}
