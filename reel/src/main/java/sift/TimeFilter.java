package main.java.sift;

import java.util.Date;
import java.util.stream.Stream;

/** Time filter. */
public class TimeFilter implements Filter {
    private final Date from;
    private final Date to;

    public TimeFilter(final Date from, final Date to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public Stream<Show> filter(final Stream<Show> shows) {
        return shows
            .filter(s -> s.getDateTime().after(this.from))
            .filter(s -> s.getDateTime().before(this.to));
    }
}
