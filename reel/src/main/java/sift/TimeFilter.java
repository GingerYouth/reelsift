package main.java.sift;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

/** Time filter. */
public class TimeFilter implements Filter {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");
    private final LocalTime from;
    private final LocalTime to;

    public TimeFilter(final String from, final String to) {
        this(LocalTime.parse(from), LocalTime.parse(to));
    }

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
