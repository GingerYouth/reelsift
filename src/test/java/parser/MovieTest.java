package parser;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for Movie record.
 */
public class MovieTest {

    @Test
    public void testMovieCreation() {
        List<String> genres = Arrays.asList("Action", "Drama");
        Movie movie = new Movie("Test Movie", genres, "A test description", "Good verdict");

        assertEquals("Test Movie", movie.title());
        assertEquals(genres, movie.genres());
        assertEquals("A test description", movie.description());
        assertEquals("Good verdict", movie.verdict());
    }

    @Test
    public void testLlmInputFormat() {
        List<String> genres = Arrays.asList("Action", "Drama");
        Movie movie = new Movie("Test Movie", genres, "A test description", "Good verdict");

        String expected = """
            Title: Test Movie
            Genres: Action, Drama
            Description: A test description
            Verdict: Good verdict
            ---
            """;

        assertEquals(expected, movie.llmInputFormat());
    }

    @Test
    public void testLlmInputFormatEmptyGenres() {
        List<String> genres = List.of();
        Movie movie = new Movie("Test Movie", genres, "A test description", "Good verdict");

        String expected = """
            Title: Test Movie
            Genres: N/A
            Description: A test description
            Verdict: Good verdict
            ---
            """;

        assertEquals(expected, movie.llmInputFormat());
    }

    @Test
    public void testGenresString() {
        // Testing private method via public method
        List<String> genres = Arrays.asList("Action", "Drama");
        Movie movie = new Movie("Test", genres, "Desc", "Verdict");

        String expected = """
            Title: Test
            Genres: Action, Drama
            Description: Desc
            Verdict: Verdict
            ---
            """;

        assertTrue(movie.llmInputFormat().contains("Genres: Action, Drama"));
    }
}