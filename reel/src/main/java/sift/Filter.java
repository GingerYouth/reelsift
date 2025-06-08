package main.java.sift;

import java.util.stream.Stream;

/** Filter interface. */
public interface Filter {
    Stream<Show> filter(Stream<Show> shows);
}
