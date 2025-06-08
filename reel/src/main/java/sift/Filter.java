package main.java.sift;

import java.util.stream.Stream;

public interface Filter {
    Stream<Show> filter(Stream<Show> seances);
}
