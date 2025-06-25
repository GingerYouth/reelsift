package main.java.sift.filters;

import java.util.List;
import java.util.stream.Stream;
import main.java.sift.Session;

/** Excluded genres. */
public class ExcludedGenres implements Filter {
    private final List<String> excluded;

    public ExcludedGenres(final List<String> excluded) {
        this.excluded = excluded;
    }

    @Override
    public Stream<Session> filter(final Stream<Session> shows) {
        return shows
            .filter(s -> this.excluded.stream().noneMatch(e -> s.genres().contains(e)));
    }
}
