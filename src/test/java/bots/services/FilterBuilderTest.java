package bots.services;

import filters.Filters;
import filters.Genre;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import parser.Session;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for {@link FilterBuilder}.
 * Tests filter chain construction based on user preferences.
 */
final class FilterBuilderTest {

    @Test
    void buildFiltersReturnsEmptyFiltersWhenNoPreferencesSet() {
        final UserService userService = new UserService();
        final FilterBuilder builder = new FilterBuilder(userService);
        final long chatId = randomChatId();
        final Filters filters = builder.buildFilters(chatId);
        final List<Session> sessions = List.of(
            randomSession("Фильм А", List.of("Драма"), "14:00", false),
            randomSession("Фильм Б", List.of("Комедия"), "18:00", true)
        );

        assertThat(
            "cant return all sessions when no filters configured",
            filters.filter(sessions),
            hasSize(2)
        );
    }

    @Test
    void buildFiltersAppliesTimeFilter() {
        final UserService userService = new UserService();
        final long chatId = randomChatId();
        userService.setTimeFilter(chatId, "12:00-16:00");
        final FilterBuilder builder = new FilterBuilder(userService);
        final Filters filters = builder.buildFilters(chatId);
        final List<Session> sessions = List.of(
            randomSession("Фильм А", List.of("Драма"), "10:00", false),
            randomSession("Фильм Б", List.of("Комедия"), "14:00", false),
            randomSession("Фильм В", List.of("Боевик"), "20:00", false)
        );

        assertThat(
            "cant filter sessions by time range",
            filters.filter(sessions),
            hasSize(1)
        );
    }

    @Test
    void buildFiltersAppliesMandatoryGenresFilter() {
        final UserService userService = new UserService();
        final long chatId = randomChatId();
        userService.setMandatoryGenres(chatId, EnumSet.of(Genre.DRAMA));
        final FilterBuilder builder = new FilterBuilder(userService);
        final Filters filters = builder.buildFilters(chatId);
        final List<Session> sessions = List.of(
            randomSession("Фильм А", List.of("Драма"), "14:00", false),
            randomSession("Фильм Б", List.of("Комедия"), "14:00", false),
            randomSession("Фильм В", List.of("Драма", "Комедия"), "14:00", false)
        );

        assertThat(
            "cant filter sessions by mandatory genres",
            filters.filter(sessions),
            hasSize(2)
        );
    }

    @Test
    void buildFiltersAppliesExcludedGenresFilter() {
        final UserService userService = new UserService();
        final long chatId = randomChatId();
        userService.setExcludedGenres(chatId, EnumSet.of(Genre.HORROR));
        final FilterBuilder builder = new FilterBuilder(userService);
        final Filters filters = builder.buildFilters(chatId);
        final List<Session> sessions = List.of(
            randomSession("Фильм А", List.of("Ужасы"), "14:00", false),
            randomSession("Фильм Б", List.of("Комедия"), "14:00", false),
            randomSession("Фильм В", List.of("Драма"), "14:00", false)
        );

        assertThat(
            "cant filter out sessions with excluded genres",
            filters.filter(sessions),
            hasSize(2)
        );
    }

    @Test
    void buildFiltersCombinesMultipleFilters() {
        final UserService userService = new UserService();
        final long chatId = randomChatId();
        userService.setTimeFilter(chatId, "12:00-20:00");
        userService.setExcludedGenres(chatId, EnumSet.of(Genre.HORROR));
        final FilterBuilder builder = new FilterBuilder(userService);
        final Filters filters = builder.buildFilters(chatId);
        final List<Session> sessions = List.of(
            randomSession("Утренний фильм", List.of("Драма"), "10:00", false),
            randomSession("Ужасы днём", List.of("Ужасы"), "14:00", false),
            randomSession("Комедия днём", List.of("Комедия"), "15:00", false),
            randomSession("Вечерний фильм", List.of("Боевик"), "22:00", false)
        );

        assertThat(
            "cant combine multiple filters correctly",
            filters.filter(sessions),
            hasSize(1)
        );
    }

    @Test
    void buildFiltersUsesCorrectChatId() {
        final UserService userService = new UserService();
        final long chatId1 = randomChatId();
        final long chatId2 = randomChatId();
        userService.setTimeFilter(chatId1, "10:00-12:00");
        userService.setExcludedGenres(chatId2, EnumSet.of(Genre.HORROR));
        final FilterBuilder builder = new FilterBuilder(userService);
        final Filters filters1 = builder.buildFilters(chatId1);
        final Filters filters2 = builder.buildFilters(chatId2);
        final List<Session> sessions = List.of(
            randomSession("Фильм", List.of("Драма"), "11:00", false)
        );

        assertThat(
            "cant apply correct filters for chatId1",
            filters1.filter(sessions),
            hasSize(1)
        );
        assertThat(
            "cant apply correct filters for chatId2",
            filters2.filter(sessions),
            hasSize(1)
        );
    }

    @Test
    void buildFiltersHandlesMultipleMandatoryGenres() {
        final UserService userService = new UserService();
        final long chatId = randomChatId();
        userService.setMandatoryGenres(chatId, EnumSet.of(Genre.DRAMA, Genre.COMEDY));
        final FilterBuilder builder = new FilterBuilder(userService);
        final Filters filters = builder.buildFilters(chatId);
        final List<Session> sessions = List.of(
            randomSession("Только драма", List.of("Драма"), "14:00", false),
            randomSession("Только комедия", List.of("Комедия"), "14:00", false),
            randomSession("Боевик", List.of("Боевик"), "14:00", false)
        );

        assertThat(
            "cant filter sessions requiring any of mandatory genres",
            filters.filter(sessions),
            hasSize(2)
        );
    }

    @Test
    void buildFiltersReturnsEmptyListWhenAllFiltered() {
        final UserService userService = new UserService();
        final long chatId = randomChatId();
        userService.setTimeFilter(chatId, "00:00-01:00");
        final FilterBuilder builder = new FilterBuilder(userService);
        final Filters filters = builder.buildFilters(chatId);
        final List<Session> sessions = List.of(
            randomSession("Фильм А", List.of("Драма"), "14:00", false),
            randomSession("Фильм Б", List.of("Комедия"), "18:00", false)
        );

        assertThat(
            "cant return empty list when all sessions filtered out",
            filters.filter(sessions),
            is(empty())
        );
    }

    private static long randomChatId() {
        return new Random().nextLong(1_000_000_000L);
    }

    private static Session randomSession(
        final String name, final List<String> genres,
        final String time, final boolean withSubs
    ) {
        final String[] parts = time.split(":");
        final int hour = Integer.parseInt(parts[0]);
        final int minute = Integer.parseInt(parts[1]);
        return new Session(
            LocalDateTime.now().withHour(hour).withMinute(minute),
            name,
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            genres,
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            new Random().nextInt(1000),
            UUID.randomUUID().toString(),
            withSubs
        );
    }
}
