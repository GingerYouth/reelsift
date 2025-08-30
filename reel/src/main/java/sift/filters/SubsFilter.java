package main.java.sift.filters;

import java.util.stream.Stream;
import main.java.sift.Session;

/** Filter by sessions with subtitles. */
public class SubsFilter implements Filter {
    @Override
    public Stream<Session> filter(final Stream<Session> shows) {
        return shows.filter(Session::russianSubtitlesSession);
    }
}
