package main.java.sift.filters;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Genres. */
public enum Genre {
    ANIME("Аниме", "аниме"),
    ACTION("Боевик", "боевик"),
    BIOPIC("Биография", "биография", "байопик", "биографический"),
    WESTERN("Вестерн", "вестерн"),
    WAR("Военный", "военный"),
    DETECTIVE("Детектив", "детектив", "детективный"),
    CHILDREN("Детский", "детский"),
    DOCUMENTARY("Документальный", "документальный"),
    DRAMA("Драма", "драма", "драматический"),
    HISTORY("Исторический", "исторический"),
    COMEDY("Комедия", "комедия"),
    CRIME("Криминальный", "криминальный", "криминал"),
    ROMANCE("Мелодрама", "мелодрама", "романтика", "романтичный"),
    MYSTICAL("Мистика", "мистика", "мистический"),
    MUSIC("Музыкальный", "музыкальный"),
    MUSICAL("Мюзикл", "мюзикл"),
    ANIMATION("Мультфильм", "анимация", "мультфильм"),
    ADVENTURE("Приключения", "приключения", "приключенческий"),
    ROMCOM("Ромком", "ромком", "романтическая комедия"),
    FAMILY("Семейный", "семейный"),
    TRAGICOMEDY("Трагикомедия", "трагикомедия"),
    THRILLER("Триллер", "триллер"),
    HORROR("Ужасы", "ужасы", "ужас", "хоррор"),
    SCIENCE_FICTION("Фантастика", "фантастика", "научная фантастика", "фантастический"),
    FANTASY("Фэнтези", "фэнтези", "фентези"),
    ADAPTATION("Экранизация", "экранизация");

    private final String displayName;
    private final Set<String> aliases;

    Genre(final String displayName, final String... aliases) {
        this.displayName = displayName;
        this.aliases = Set.of(aliases);
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public static List<String> getDisplayNames() {
        return Arrays.stream(values())
            .map(Genre::getDisplayName)
            .collect(Collectors.toList());
    }

    /** Result of genres parsing. */
    public record ParseResult(Set<Genre> validGenres, Set<String> invalidGenres) {}

    public static ParseResult parseGenres(final String input) {
        if (input == null || input.isBlank()) {
            return new ParseResult(Collections.emptySet(), Collections.emptySet());
        }

        final Set<Genre> validGenres = EnumSet.noneOf(Genre.class);
        final Set<String> invalidGenres = new HashSet<>();

        for (final String genreStr : input.split(",")) {
            final String normalized = genreStr.trim().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty()) {
                continue;
            }

            final Optional<Genre> genre = fromString(normalized);
            if (genre.isPresent()) {
                validGenres.add(genre.get());
            } else {
                invalidGenres.add(genreStr.trim());
            }
        }

        return new ParseResult(validGenres, invalidGenres);
    }

    public static Optional<Genre> fromString(final String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        final String normalized = text.trim().toLowerCase(Locale.ROOT);
        for (final Genre genre : values()) {
            if (genre.name().equalsIgnoreCase(normalized)) {
                return Optional.of(genre);
            }

            for (final String alias : genre.aliases) {
                if (alias.equalsIgnoreCase(normalized)) {
                    return Optional.of(genre);
                }
            }
        }
        return Optional.empty();
    }

    public static String toStringOrDefault(final Set<Genre> genres, final String defaultValue) {
        if (genres == null || genres.isEmpty()) {
            return defaultValue;
        }
        return genres.stream()
            .map(Genre::getDisplayName)
            .collect(Collectors.joining(", "));
    }
}
