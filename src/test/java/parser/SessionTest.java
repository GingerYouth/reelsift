package parser;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests for Session record.
 */
public class SessionTest {

    @Test
    public void testSessionCreation() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 1, 10, 30);
        List<String> genres = Arrays.asList("Action", "Drama");
        Session session = new Session(dateTime, "Test Movie", "Description", "Verdict",
                                    genres, "Cinema", "Address", 500, "link", true);

        assertEquals(dateTime, session.dateTime());
        assertEquals("Test Movie", session.name());
        assertEquals("Description", session.description());
        assertEquals("Verdict", session.verdict());
        assertEquals(genres, session.genres());
        assertEquals("Cinema", session.cinema());
        assertEquals("Address", session.address());
        assertEquals(500, session.price());
        assertEquals("link", session.link());
        assertTrue(session.russianSubtitlesSession());
    }

    @Test
    public void testToJsonArray() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 1, 10, 30);
        List<String> genres = Arrays.asList("Action", "Drama");
        Session session1 = new Session(dateTime, "Movie1", "Desc1", "Verdict1",
                                     genres, "Cinema1", "Addr1", 500, "link1", true);
        Session session2 = new Session(dateTime.plusHours(2), "Movie2", "Desc2", "Verdict2",
                                     genres, "Cinema2", "Addr2", 600, "link2", false);

        List<Session> sessions = Arrays.asList(session1, session2);
        String json = Session.toJson(sessions);

        assertNotNull(json);
        assertTrue(json.startsWith("["));
        assertTrue(json.endsWith("]"));
    }

    @Test
    public void testFromJsonArray() {
        String json = "[{\"dateTime\":\"2024-01-01T10:30\",\"name\":\"Test Movie\",\"description\":\"Desc\",\"verdict\":\"Verdict\",\"genres\":[\"Action\",\"Drama\"],\"cinema\":\"Cinema\",\"address\":\"Address\",\"price\":500,\"link\":\"link\",\"russianSubtitlesSession\":true,\"imageUrl\":\"\"}]";

        List<Session> sessions = Session.fromJsonArray(json);

        assertEquals(1, sessions.size());
        Session session = sessions.get(0);
        assertEquals("Test Movie", session.name());
        assertEquals("Cinema", session.cinema());
        assertEquals(500, session.price());
        assertTrue(session.russianSubtitlesSession());
    }

    @Test
    public void testFromJsonArrayEmpty() {
        List<Session> sessions = Session.fromJsonArray("");
        assertTrue(sessions.isEmpty());

        sessions = Session.fromJsonArray(null);
        assertTrue(sessions.isEmpty());
    }

    @Test
    public void testFromJsonArraySingleObject() {
        String json = "{\"dateTime\":\"2024-01-01T10:30\",\"name\":\"Test Movie\",\"description\":\"Desc\",\"verdict\":\"Verdict\",\"genres\":[\"Action\",\"Drama\"],\"cinema\":\"Cinema\",\"address\":\"Address\",\"price\":500,\"link\":\"link\",\"imageUrl\":\"\"}";

        List<Session> sessions = Session.fromJsonArray(json);

        assertEquals(1, sessions.size());
        assertEquals("Test Movie", sessions.get(0).name());
    }

    @Test
    public void testToStringWithSessions() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 1, 10, 30);
        List<String> genres = Arrays.asList("Action", "Drama");
        Session session = new Session(dateTime, "Test Movie", "Description", "Verdict",
                                    genres, "Cinema", "Address", 500, "link", true);

        String result = Session.toString(List.of(session), 10);

        assertTrue(result.contains("Film: Test Movie"));
        assertTrue(result.contains("Genres: Action, Drama"));
        assertTrue(result.contains("Verdict: Verdict"));
        assertTrue(result.contains("Description: Description"));
        assertTrue(result.contains("Cinema"));
        assertTrue(result.contains("500 RUB"));
    }

    @Test
    public void testToStringEmptyList() {
        String result = Session.toString(Collections.emptyList(), 10);
        assertEquals("", result);
    }

    @Test
    public void testMovieGroup() {
        List<String> genres = Arrays.asList("Action", "Drama");
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 1, 10, 30);
        Session session = new Session(dateTime, "Movie", "Desc", "Verdict",
                                    genres, "Cinema", "Addr", 500, "link", false);

        Session.MovieGroup group = new Session.MovieGroup("Movie", genres, "Verdict", "Desc", List.of(session), "");

        assertEquals("Movie", group.movieName());
        assertEquals(genres, group.genres());
        assertEquals("Verdict", group.verdict());
        assertEquals("Desc", group.description());
        assertEquals(1, group.sessions().size());
    }

    @Test
    public void toSplitStringsReturnsEmptyListForEmptyInput() {
        assertThat(
            "cant return empty list for empty session input",
            Session.toSplitStrings(Collections.emptyList(), 10),
            is(empty())
        );
    }

    @Test
    public void toSplitStringsAlwaysReturnsOmittedSessions() {
        final String movieName = "Film " + UUID.randomUUID();
        final List<Session> sessions = IntStream.range(0, 3)
            .mapToObj(i -> new Session(
                LocalDateTime.of(2024, 1, 1, 10 + i, 0),
                movieName, "Desc", "Verdict",
                List.of("Drama"), "Cinema" + i, "Addr" + i, 500, "link", false
            ))
            .toList();
        assertThat(
            "should always return all sessions in the omittedSessions list",
            Session.toSplitStrings(sessions, 10).get(0).omittedSessions(),
            hasSize(3)
        );
    }

    @Test
    public void toSplitStringsReturnsOmittedSessionsAtThreshold() {
        final String movieName = "Film " + UUID.randomUUID();
        final List<Session> sessions = IntStream.range(0, 10)
            .mapToObj(i -> new Session(
                LocalDateTime.of(2024, 1, 1, 10, i),
                movieName, "Desc", "Verdict",
                List.of("Drama"), "Cinema", "Addr", 500, "link", false
            ))
            .toList();
        assertThat(
            "cant return omitted sessions when count reaches threshold",
            Session.toSplitStrings(sessions, 10).get(0).omittedSessions(),
            hasSize(10)
        );
    }

    @Test
    public void formatSessionLinesContainsCinemaName() {
        final String cinemaName = "Cinema " + UUID.randomUUID();
        final List<Session> sessions = List.of(
            new Session(
                LocalDateTime.of(2024, 1, 1, 10, 0),
                "Film", "Desc", "Verdict",
                List.of("Drama"), cinemaName, "Addr", 500, "link", false
            )
        );
        assertThat(
            "cant include cinema name in formatted session lines",
            Session.formatSessionLines(sessions),
            containsString(cinemaName)
        );
    }

    @Test
    public void formatSessionLinesContainsDateTime() {
        final LocalDateTime dateTime = LocalDateTime.of(2024, 3, 15, 14, 30);
        final List<Session> sessions = List.of(
            new Session(
                dateTime, "Film", "Desc", "Verdict",
                List.of("Drama"), "Cinema", "Addr", 500, "link", false
            )
        );
        assertThat(
            "cant include date/time in formatted session lines",
            Session.formatSessionLines(sessions),
            containsString("2024-03-15T14:30")
        );
    }

    @Test
    public void formatSessionLinesReturnsHeaderForEmptyInput() {
        assertThat(
            "cant return header for empty session list",
            Session.formatSessionLines(Collections.emptyList()),
            containsString("Сеансы")
        );
    }

    @Test
    public void toSplitStringsCombinesSessionsIntoOneMessage() {
        final String movieName = "Film " + UUID.randomUUID();
        final List<Session> sessions = IntStream.range(0, 3)
            .mapToObj(i -> new Session(
                LocalDateTime.of(2024, 1, 1, 10 + i, 0),
                movieName, "Desc", "Verdict",
                List.of("Drama"), "Cinema" + i, "Addr" + i, 500, "link", false
            ))
            .toList();
        assertThat(
            "cant combine all sessions of one movie into a single message",
            Session.toSplitStrings(sessions, 10),
            hasSize(1)
        );
    }

    @Test
    public void toSplitStringsCombinedMessageContainsFilmName() {
        final String movieName = "Film " + UUID.randomUUID();
        final List<Session> sessions = List.of(
            new Session(
                LocalDateTime.of(2024, 1, 1, 10, 0),
                movieName, "Desc", "Verdict",
                List.of("Drama"), "Cinema", "Addr", 500, "link", false
            )
        );
        assertThat(
            "cant include film name in combined message",
            Session.toSplitStrings(sessions, 10).get(0).text(),
            containsString(movieName)
        );
    }

    @Test
    public void toSplitStringsCombinedMessageExcludesSessionDetails() {
        final String cinemaName = "Cinema " + UUID.randomUUID();
        final List<Session> sessions = List.of(
            new Session(
                LocalDateTime.of(2024, 1, 1, 10, 0),
                "Film", "Desc", "Verdict",
                List.of("Drama"), cinemaName, "Addr", 500, "link", false
            )
        );
        assertThat(
            "should not include session details in the main message text",
            Session.toSplitStrings(sessions, 10).get(0).text(),
            not(containsString(cinemaName))
        );
    }

    @Test
    public void toSplitStringsOmitsSessionsWhenCountReachesThreshold() {
        final String cinemaName = "Cinema " + UUID.randomUUID();
        final List<Session> sessions = IntStream.range(0, 10)
            .mapToObj(i -> new Session(
                LocalDateTime.of(2024, 1, 1, 10 + i, 0),
                "Film", "Desc", "Verdict",
                List.of("Drama"), cinemaName, "Addr", 500, "link", false
            ))
            .toList();
        assertThat(
            "cant omit sessions when count reaches threshold",
            Session.toSplitStrings(sessions, 10).get(0).text(),
            not(containsString(cinemaName))
        );
    }

    @Test
    public void toSplitStringsIncludesFilmInfoWhenSessionsOmitted() {
        final String movieName = "Film " + UUID.randomUUID();
        final List<Session> sessions = IntStream.range(0, 12)
            .mapToObj(i -> new Session(
                LocalDateTime.of(2024, 1, 1, 10, i),
                movieName, "Desc", "Verdict",
                List.of("Drama"), "Cinema", "Addr", 500, "link", false
            ))
            .toList();
        assertThat(
            "cant include film info when sessions are omitted",
            Session.toSplitStrings(sessions, 10).get(0).text(),
            containsString(movieName)
        );
    }

    @Test
    public void toSplitStringsReturnsOneMessagePerMovie() {
        final List<Session> sessions = List.of(
            new Session(
                LocalDateTime.of(2024, 1, 1, 10, 0),
                "Film A " + UUID.randomUUID(), "Desc", "Verdict",
                List.of("Drama"), "Cinema", "Addr", 500, "link", false
            ),
            new Session(
                LocalDateTime.of(2024, 1, 1, 12, 0),
                "Film B " + UUID.randomUUID(), "Desc", "Verdict",
                List.of("Action"), "Cinema", "Addr", 600, "link", false
            )
        );
        assertThat(
            "cant produce exactly one message per movie",
            Session.toSplitStrings(sessions, 10),
            hasSize(2)
        );
    }

    @Test
    public void toSplitStringsAlwaysHidesSessions() {
        final String cinemaName = "Cinema " + UUID.randomUUID();
        final List<Session> sessions = IntStream.range(0, 9)
            .mapToObj(i -> new Session(
                LocalDateTime.of(2024, 1, 1, 10 + i, 0),
                "Film", "Desc", "Verdict",
                List.of("Drama"), cinemaName, "Addr", 500, "link", false
            ))
            .toList();
        assertThat(
            "should always hide session details from the main text",
            Session.toSplitStrings(sessions, 10).get(0).text(),
            not(containsString(cinemaName))
        );
    }

    @Test
    public void toStringOmitsSessionsWhenCountReachesThreshold() {
        final String cinemaName = "Cinema " + UUID.randomUUID();
        final List<Session> sessions = IntStream.range(0, 11)
            .mapToObj(i -> new Session(
                LocalDateTime.of(2024, 1, 1, 10, i),
                "Film", "Desc", "Verdict",
                List.of("Drama"), cinemaName, "Addr", 500, "link", false
            ))
            .toList();
        assertThat(
            "cant omit session lines in toString when count exceeds threshold",
            Session.toString(sessions, 10),
            not(containsString(cinemaName))
        );
    }
}