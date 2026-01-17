package filters;

import org.junit.Test;
import static org.junit.Assert.*;

import parser.Session;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Unit tests for TimeFilter.
 */
public class TimeFilterTest {

    @Test
    public void testConstructorWithStrings() {
        TimeFilter filter = new TimeFilter("10:00", "18:00");
        // Constructor should not throw exception
        assertNotNull(filter);
    }

    @Test(expected = Exception.class)
    public void testConstructorWithInvalidTimeString() {
        new TimeFilter("invalid", "18:00");
    }

    @Test
    public void testConstructorWithLocalTime() {
        LocalTime from = LocalTime.of(10, 0);
        LocalTime to = LocalTime.of(18, 0);
        TimeFilter filter = new TimeFilter(from, to);
        assertNotNull(filter);
    }

    @Test
    public void testFilterWithinTimeRange() {
        TimeFilter filter = new TimeFilter("10:00", "18:00");

        // Create sessions within and outside the range
        Session session1 = createTestSession("Movie1", LocalDateTime.of(2024, 1, 1, 9, 30)); // Before
        Session session2 = createTestSession("Movie2", LocalDateTime.of(2024, 1, 1, 12, 0));  // Within
        Session session3 = createTestSession("Movie3", LocalDateTime.of(2024, 1, 1, 19, 0));  // After

        List<Session> sessions = Arrays.asList(session1, session2, session3);
        Stream<Session> result = filter.filter(sessions.stream());

        List<Session> filtered = result.toList();
        assertEquals(1, filtered.size());
        assertEquals("Movie2", filtered.get(0).name());
    }

    @Test
    public void testFilterBoundaryTimes() {
        TimeFilter filter = new TimeFilter("10:00", "18:00");

        // Create sessions at boundary times
        Session session1 = createTestSession("Movie1", LocalDateTime.of(2024, 1, 1, 10, 0)); // At start (excluded by isAfter)
        Session session2 = createTestSession("Movie2", LocalDateTime.of(2024, 1, 1, 18, 0)); // At end (excluded by isBefore)

        List<Session> sessions = Arrays.asList(session1, session2);
        Stream<Session> result = filter.filter(sessions.stream());

        List<Session> filtered = result.toList();
        assertTrue(filtered.isEmpty());
    }

    @Test
    public void testFilterEmptyStream() {
        TimeFilter filter = new TimeFilter("10:00", "18:00");
        Stream<Session> result = filter.filter(Stream.empty());
        assertEquals(0, result.count());
    }

    private Session createTestSession(String name, LocalDateTime dateTime) {
        return new Session(
            dateTime,
            name,
            "Description",
            "Verdict",
            Arrays.asList("Action"),
            "Cinema",
            "Address",
            500,
            "link",
            false
        );
    }
}