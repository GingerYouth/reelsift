package main.java.sift.filters;

import main.java.sift.Session;

import java.util.stream.Stream;

/** Filter interface. */
public interface Filter {
    Stream<Session> filter(Stream<Session> shows);
}
