package main.java.sift.filters;

import main.java.sift.Session;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

/** Time filter. */
@SuppressWarnings("PMD.ShortVariable")
public class TimeFilter implements Filter {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");
    private final LocalTime from;
    private final LocalTime to;

    public TimeFilter(final String from, final String to) {
        this(LocalTime.parse(from, TIME_FORMATTER), LocalTime.parse(to, TIME_FORMATTER));
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
