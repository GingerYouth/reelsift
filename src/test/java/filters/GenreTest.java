package filters;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Unit tests for Genre enum.
 */
public class GenreTest {

    @Test
    public void testGetDisplayName() {
        assertEquals("Боевик", Genre.ACTION.getDisplayName());
        assertEquals("Драма", Genre.DRAMA.getDisplayName());
        assertEquals("Комедия", Genre.COMEDY.getDisplayName());
    }

    @Test
    public void testGetDisplayNames() {
        List<String> displayNames = Genre.getDisplayNames();
        assertTrue(displayNames.contains("Боевик"));
        assertTrue(displayNames.contains("Драма"));
        assertTrue(displayNames.contains("Комедия"));
        assertEquals(26, displayNames.size()); // Based on enum values
    }

    @Test
    public void testFromStringValidGenre() {
        Optional<Genre> genre = Genre.fromString("Боевик");
        assertTrue(genre.isPresent());
        assertEquals(Genre.ACTION, genre.get());

        genre = Genre.fromString("боевик");
        assertTrue(genre.isPresent());
        assertEquals(Genre.ACTION, genre.get());
    }

    @Test
    public void testFromStringValidAlias() {
        Optional<Genre> genre = Genre.fromString("аниме");
        assertTrue(genre.isPresent());
        assertEquals(Genre.ANIME, genre.get());

        genre = Genre.fromString("романтика");
        assertTrue(genre.isPresent());
        assertEquals(Genre.ROMANCE, genre.get());
    }

    @Test
    public void testFromStringInvalidGenre() {
        Optional<Genre> genre = Genre.fromString("invalidgenre");
        assertFalse(genre.isPresent());

        genre = Genre.fromString("");
        assertFalse(genre.isPresent());

        genre = Genre.fromString(null);
        assertFalse(genre.isPresent());
    }

    @Test
    public void testParseGenres() {
        String input = "Боевик, Драма, invalid";
        Genre.ParseResult result = Genre.parseGenres(input);

        assertEquals(2, result.validGenres().size());
        assertTrue(result.validGenres().contains(Genre.ACTION));
        assertTrue(result.validGenres().contains(Genre.DRAMA));

        assertEquals(1, result.invalidGenres().size());
        assertTrue(result.invalidGenres().contains("invalid"));
    }

    @Test
    public void testParseGenresEmpty() {
        Genre.ParseResult result = Genre.parseGenres("");
        assertTrue(result.validGenres().isEmpty());
        assertTrue(result.invalidGenres().isEmpty());

        result = Genre.parseGenres(null);
        assertTrue(result.validGenres().isEmpty());
        assertTrue(result.invalidGenres().isEmpty());
    }

    @Test
    public void testParseGenresWithSpaces() {
        String input = " Боевик , Драма ";
        Genre.ParseResult result = Genre.parseGenres(input);

        assertEquals(2, result.validGenres().size());
        assertTrue(result.validGenres().contains(Genre.ACTION));
        assertTrue(result.validGenres().contains(Genre.DRAMA));
        assertTrue(result.invalidGenres().isEmpty());
    }

    @Test
    public void testToStringOrDefault() {
        Set<Genre> genres = Set.of(Genre.ACTION, Genre.DRAMA);
        String result = Genre.toStringOrDefault(genres, "None");
        assertTrue(result.contains("Боевик"));
        assertTrue(result.contains("Драма"));

        result = Genre.toStringOrDefault(Set.of(), "None");
        assertEquals("None", result);

        result = Genre.toStringOrDefault(null, "None");
        assertEquals("None", result);
    }

    @Test
    public void testParseResultRecord() {
        Set<Genre> valid = Set.of(Genre.ACTION);
        Set<String> invalid = Set.of("invalid");
        Genre.ParseResult result = new Genre.ParseResult(valid, invalid);

        assertEquals(valid, result.validGenres());
        assertEquals(invalid, result.invalidGenres());
    }
}