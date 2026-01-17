package filters;

import parser.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Filters. */
public class Filters {
    @SuppressWarnings("PMD.AvoidFieldNameMatchingTypeName")
    private final List<Filter> filters;

    public Filters() {
        this.filters = new ArrayList<>();
    }

    public Filters(final List<Filter> filters) {
        this.filters = filters;
    }

    public Filters(final Filter... filters) {
        this.filters = List.of(filters);
    }

    public void addFilter(final Filter filter) {
        this.filters.add(filter);
    }

    public List<Session> filter(final List<Session> shows) {
        Stream<Session> stream = shows.stream();
        for (final Filter filter : filters) {
            stream = filter.filter(stream);
        }
        return stream.toList();
    }
}
