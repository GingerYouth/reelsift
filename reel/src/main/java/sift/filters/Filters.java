package main.java.sift.filters;

import main.java.sift.Session;

import java.util.List;
import java.util.stream.Stream;

/** Filters. */
public class Filters {
    private final List<Filter> filters;

    public Filters(final List<Filter> filters) {
        this.filters = filters;
    }

    public Filters(final Filter... filters) {
        this.filters = List.of(filters);
    }

    public List<Session> filter(final List<Session> shows) {
        Stream<Session> stream = shows.stream();
        for (final Filter filter : filters) {
            stream = filter.filter(stream);
        }
        return stream.toList();
    }
}
