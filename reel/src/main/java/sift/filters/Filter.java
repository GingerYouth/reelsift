package main.java.sift.filters;

import java.util.stream.Stream;
import main.java.sift.Session;

/** Filter interface. */
public interface Filter {
    Stream<Session> filter(Stream<Session> shows);
}
