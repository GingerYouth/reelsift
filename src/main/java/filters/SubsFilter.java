package filters;

import parser.Session;

import java.util.stream.Stream;

/** Filter by sessions with subtitles. */
public class SubsFilter implements Filter {
    @Override
    public Stream<Session> filter(final Stream<Session> shows) {
        return shows.filter(Session::russianSubtitlesSession);
    }
}
