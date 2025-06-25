package main.java.sift.filters;

import java.util.List;
import java.util.stream.Stream;
import main.java.sift.Session;

/** Mandatory genres. */
public class MandatoryGenres implements Filter {
    private final List<String> mandatory;

    public MandatoryGenres(final List<String> mandatory) {
        this.mandatory = mandatory;
    }

    @Override
    public Stream<Session> filter(final Stream<Session> shows) {
        if (mandatory.isEmpty()) {
            return shows;
        } else {
            return shows
                .filter(s -> this.mandatory.stream().anyMatch(m -> s.genres().contains(m)));
        }
    }
}
