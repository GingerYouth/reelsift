package filters;

import org.junit.Test;
import static org.junit.Assert.*;

import parser.Session;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Unit tests for ExcludedGenres filter.
 */
public class ExcludedGenresTest {

    @Test
    public void testFilterExcludesGenres() {
        List<String> excludedGenres = Arrays.asList("Horror", "Thriller");
        ExcludedGenres filter = new ExcludedGenres(excludedGenres);

        // Create test sessions
        Session session1 = createTestSession("Movie1", Arrays.asList("Action", "Comedy"));
        Session session2 = createTestSession("Movie2", Arrays.asList("Drama", "Horror"));
        Session session3 = createTestSession("Movie3", Arrays.asList("Comedy", "Romance"));

        List<Session> sessions = Arrays.asList(session1, session2, session3);
        Stream<Session> result = filter.filter(sessions.stream());

        List<Session> filtered = result.toList();
        assertEquals(2, filtered.size());
        assertTrue(filtered.stream().anyMatch(s -> s.name().equals("Movie1")));
        assertTrue(filtered.stream().anyMatch(s -> s.name().equals("Movie3")));
        assertFalse(filtered.stream().anyMatch(s -> s.name().equals("Movie2")));
    }

    @Test
    public void testFilterNoExclusions() {
        ExcludedGenres filter = new ExcludedGenres(List.of());

        Session session1 = createTestSession("Movie1", Arrays.asList("Action", "Comedy"));
        Session session2 = createTestSession("Movie2", Arrays.asList("Drama", "Horror"));

        List<Session> sessions = Arrays.asList(session1, session2);
        Stream<Session> result = filter.filter(sessions.stream());

        List<Session> filtered = result.toList();
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterAllExcluded() {
        List<String> excludedGenres = Arrays.asList("Action", "Comedy", "Drama", "Horror");
        ExcludedGenres filter = new ExcludedGenres(excludedGenres);

        Session session1 = createTestSession("Movie1", Arrays.asList("Action", "Comedy"));
        Session session2 = createTestSession("Movie2", Arrays.asList("Drama", "Horror"));

        List<Session> sessions = Arrays.asList(session1, session2);
        Stream<Session> result = filter.filter(sessions.stream());

        List<Session> filtered = result.toList();
        assertTrue(filtered.isEmpty());
    }

    private Session createTestSession(String name, List<String> genres) {
        return new Session(
            LocalDateTime.now(),
            name,
            "Description",
            "Verdict",
            genres,
            "Cinema",
            "Address",
            500,
            "link",
            false
        );
    }
}