package filters;

import org.junit.Test;
import static org.junit.Assert.*;

import parser.Session;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Unit tests for MandatoryGenres filter.
 */
public class MandatoryGenresTest {

    @Test
    public void testFilterWithMandatoryGenres() {
        List<String> mandatoryGenres = Arrays.asList("Action", "Drama");
        MandatoryGenres filter = new MandatoryGenres(mandatoryGenres);

        // Create test sessions
        Session session1 = createTestSession("Movie1", Arrays.asList("Action", "Comedy"));
        Session session2 = createTestSession("Movie2", Arrays.asList("Drama", "Romance"));
        Session session3 = createTestSession("Movie3", Arrays.asList("Comedy", "Horror"));

        List<Session> sessions = Arrays.asList(session1, session2, session3);
        Stream<Session> result = filter.filter(sessions.stream());

        List<Session> filtered = result.toList();
        assertEquals(2, filtered.size());
        assertTrue(filtered.stream().anyMatch(s -> s.name().equals("Movie1")));
        assertTrue(filtered.stream().anyMatch(s -> s.name().equals("Movie2")));
    }

    @Test
    public void testFilterNoMandatoryGenres() {
        MandatoryGenres filter = new MandatoryGenres(List.of());

        Session session1 = createTestSession("Movie1", Arrays.asList("Action", "Comedy"));
        Session session2 = createTestSession("Movie2", Arrays.asList("Drama", "Romance"));

        List<Session> sessions = Arrays.asList(session1, session2);
        Stream<Session> result = filter.filter(sessions.stream());

        List<Session> filtered = result.toList();
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterNoMatches() {
        List<String> mandatoryGenres = Arrays.asList("Sci-Fi");
        MandatoryGenres filter = new MandatoryGenres(mandatoryGenres);

        Session session1 = createTestSession("Movie1", Arrays.asList("Action", "Comedy"));
        Session session2 = createTestSession("Movie2", Arrays.asList("Drama", "Romance"));

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