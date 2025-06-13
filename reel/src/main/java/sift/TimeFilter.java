package main.java.sift;

import java.time.LocalTime;
import java.util.stream.Stream;

/** Time filter. */
public class TimeFilter implements Filter {
    private final LocalTime from;
    private final LocalTime to;

    public TimeFilter(final LocalTime from, final LocalTime to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public Stream<Session> filter(final Stream<Session> shows) {
        return shows
            .filter(s -> s.dateTime().isAfter(this.from))
            .filter(s -> s.dateTime().isBefore(this.to));
    }
}
