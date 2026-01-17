package filters;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;
import parser.Session;

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
            .filter(s -> s.dateTime().toLocalTime().isAfter(this.from))
            .filter(s -> s.dateTime().toLocalTime().isBefore(this.to));
    }
}
