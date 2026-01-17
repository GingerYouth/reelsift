package parser;

import org.junit.Test;
import static org.junit.Assert.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
        String json = "[{\"dateTime\":\"2024-01-01T10:30\",\"name\":\"Test Movie\",\"description\":\"Desc\",\"verdict\":\"Verdict\",\"genres\":[\"Action\",\"Drama\"],\"cinema\":\"Cinema\",\"address\":\"Address\",\"price\":500,\"link\":\"link\",\"russianSubtitlesSession\":true}]";

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
        String json = "{\"dateTime\":\"2024-01-01T10:30\",\"name\":\"Test Movie\",\"description\":\"Desc\",\"verdict\":\"Verdict\",\"genres\":[\"Action\",\"Drama\"],\"cinema\":\"Cinema\",\"address\":\"Address\",\"price\":500,\"link\":\"link\"}";

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

        String result = Session.toString(List.of(session));

        assertTrue(result.contains("Film: Test Movie"));
        assertTrue(result.contains("Genres: Action, Drama"));
        assertTrue(result.contains("Verdict: Verdict"));
        assertTrue(result.contains("Description: Description"));
        assertTrue(result.contains("Cinema"));
        assertTrue(result.contains("500 RUB"));
    }

    @Test
    public void testToStringEmptyList() {
        String result = Session.toString(Collections.emptyList());
        assertEquals("", result);
    }

    @Test
    public void testMovieGroup() {
        List<String> genres = Arrays.asList("Action", "Drama");
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 1, 10, 30);
        Session session = new Session(dateTime, "Movie", "Desc", "Verdict",
                                    genres, "Cinema", "Addr", 500, "link", false);

        Session.MovieGroup group = new Session.MovieGroup("Movie", genres, "Verdict", "Desc", List.of(session));

        assertEquals("Movie", group.movieName());
        assertEquals(genres, group.genres());
        assertEquals("Verdict", group.verdict());
        assertEquals("Desc", group.description());
        assertEquals(1, group.sessions().size());
    }
}