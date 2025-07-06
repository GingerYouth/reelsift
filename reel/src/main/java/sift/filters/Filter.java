package main.java.sift.filters;

import java.util.stream.Stream;
import main.java.sift.Session;

/** Filter interface. */
@FunctionalInterface
public interface Filter {
    Stream<Session> filter(Stream<Session> shows);
}
