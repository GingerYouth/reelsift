package filters;

import org.junit.Test;
import static org.junit.Assert.*;

import parser.Session;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for Filters class.
 */
public class FiltersTest {

    @Test
    public void testEmptyFilters() {
        Filters filters = new Filters();
        List<Session> sessions = createTestSessions();
        List<Session> result = filters.filter(sessions);
        assertEquals(sessions.size(), result.size());
    }

    @Test
    public void testConstructorWithList() {
        List<Filter> filterList = Arrays.asList(new SubsFilter(), new ExcludedGenres(Arrays.asList("Horror")));
        Filters filters = new Filters(filterList);
        assertNotNull(filters);
    }

    @Test
    public void testConstructorWithVarargs() {
        Filters filters = new Filters(new SubsFilter(), new ExcludedGenres(Arrays.asList("Horror")));
        assertNotNull(filters);
    }

    @Test
    public void testAddFilter() {
        Filters filters = new Filters();
        filters.addFilter(new SubsFilter());
        filters.addFilter(new ExcludedGenres(Arrays.asList("Horror")));

        // Test that filters were added (indirectly through filtering)
        List<Session> sessions = createTestSessions();
        List<Session> result = filters.filter(sessions);

        // Should filter out sessions without subtitles and with horror genre
        long expectedCount = sessions.stream()
            .filter(Session::russianSubtitlesSession)
            .filter(s -> !s.genres().contains("Horror"))
            .count();

        assertEquals(expectedCount, result.size());
    }

    @Test
    public void testFilterCombination() {
        // Combine multiple filters
        Filters filters = new Filters(
            new SubsFilter(),
            new ExcludedGenres(Arrays.asList("Horror")),
            new MandatoryGenres(Arrays.asList("Action"))
        );

        List<Session> sessions = Arrays.asList(
            createTestSession("Movie1", Arrays.asList("Action"), true),     // Should pass
            createTestSession("Movie2", Arrays.asList("Action"), false),    // No subs
            createTestSession("Movie3", Arrays.asList("Horror"), true),     // Horror genre
            createTestSession("Movie4", Arrays.asList("Comedy"), true)      // No Action genre
        );

        List<Session> result = filters.filter(sessions);
        assertEquals(1, result.size());
        assertEquals("Movie1", result.get(0).name());
    }

    private List<Session> createTestSessions() {
        return Arrays.asList(
            createTestSession("Movie1", Arrays.asList("Action"), true),
            createTestSession("Movie2", Arrays.asList("Drama"), false),
            createTestSession("Movie3", Arrays.asList("Horror"), true)
        );
    }

    private Session createTestSession(String name, List<String> genres, boolean hasSubs) {
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
            hasSubs
        );
    }
}