package filters;

import org.junit.Test;
import static org.junit.Assert.*;

import parser.Session;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Unit tests for SubsFilter.
 */
public class SubsFilterTest {

    @Test
    public void testFilterOnlyRussianSubtitles() {
        SubsFilter filter = new SubsFilter();

        // Create test sessions
        Session session1 = createTestSession("Movie1", false); // No subtitles
        Session session2 = createTestSession("Movie2", true);  // With subtitles
        Session session3 = createTestSession("Movie3", false); // No subtitles

        List<Session> sessions = Arrays.asList(session1, session2, session3);
        Stream<Session> result = filter.filter(sessions.stream());

        List<Session> filtered = result.toList();
        assertEquals(1, filtered.size());
        assertEquals("Movie2", filtered.get(0).name());
        assertTrue(filtered.get(0).russianSubtitlesSession());
    }

    @Test
    public void testFilterNoSessionsWithSubs() {
        SubsFilter filter = new SubsFilter();

        Session session1 = createTestSession("Movie1", false);
        Session session2 = createTestSession("Movie2", false);

        List<Session> sessions = Arrays.asList(session1, session2);
        Stream<Session> result = filter.filter(sessions.stream());

        List<Session> filtered = result.toList();
        assertTrue(filtered.isEmpty());
    }

    @Test
    public void testFilterAllSessionsWithSubs() {
        SubsFilter filter = new SubsFilter();

        Session session1 = createTestSession("Movie1", true);
        Session session2 = createTestSession("Movie2", true);

        List<Session> sessions = Arrays.asList(session1, session2);
        Stream<Session> result = filter.filter(sessions.stream());

        List<Session> filtered = result.toList();
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterEmptyStream() {
        SubsFilter filter = new SubsFilter();
        Stream<Session> result = filter.filter(Stream.empty());
        assertEquals(0, result.count());
    }

    private Session createTestSession(String name, boolean hasSubs) {
        return new Session(
            LocalDateTime.now(),
            name,
            "Description",
            "Verdict",
            Arrays.asList("Action"),
            "Cinema",
            "Address",
            500,
            "link",
            hasSubs
        );
    }
}