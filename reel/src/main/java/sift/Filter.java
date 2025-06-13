package main.java.sift;

import java.util.stream.Stream;

/** Filter interface. */
public interface Filter {
    Stream<Session> filter(Stream<Session> shows);
}
