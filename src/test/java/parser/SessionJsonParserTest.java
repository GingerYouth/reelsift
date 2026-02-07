package parser;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SessionJsonParser}.
 * Tests the Afisha API JSON response parsing.
 */
final class SessionJsonParserTest {

    @Test
    void parseSessionsReturnsEmptyListForNullInput() {
        assertThat(
            "cant return empty list for null input",
            SessionJsonParser.parseSessions(null, "", LocalDate.of(2024, 1, 15)),
            is(empty())
        );
    }

    @Test
    void parseSessionsReturnsEmptyListForEmptyString() {
        assertThat(
            "cant return empty list for empty string",
            SessionJsonParser.parseSessions("", "", LocalDate.of(2024, 1, 15)),
            is(empty())
        );
    }

    @Test
    void parseSessionsParsesMovieName() {
        final String movieName = "Тестовый фильм " + UUID.randomUUID();
        final String json = buildAfishaJson(movieName, "2024-01-15T18:00:00", "500");

        assertThat(
            "cant parse movie name from JSON",
            SessionJsonParser.parseSessions(json, "", LocalDate.of(2024, 1, 15)).get(0).name(),
            is(equalTo(movieName))
        );
    }

    @Test
    void parseSessionsParsesDateTime() {
        final String dateTime = "2024-03-20T14:30:00";
        final String json = buildAfishaJson("Фильм", dateTime, "500");

        assertThat(
            "cant parse dateTime from JSON",
            SessionJsonParser.parseSessions(
                json, "", LocalDate.of(2024, 3, 20)
            ).get(0).dateTime(),
            is(equalTo(LocalDateTime.parse(dateTime)))
        );
    }

    @Test
    void parseSessionsParsesGenres() {
        final String json = buildAfishaJsonWithGenres(
            "Фильм", "2024-01-15T18:00:00", List.of("Комедия", "Драма")
        );

        assertThat(
            "cant parse genres from JSON",
            SessionJsonParser.parseSessions(json, "", LocalDate.of(2024, 1, 15)).get(0).genres(),
            contains("Комедия", "Драма")
        );
    }

    @Test
    void parseSessionsParsesCinemaName() {
        final String cinemaName = "Кинотеатр Октябрь " + UUID.randomUUID();
        final String json = buildAfishaJsonWithPlace(
            "Фильм", "2024-01-15T18:00:00", cinemaName, "ул. Пушкина"
        );

        assertThat(
            "cant parse cinema name from JSON",
            SessionJsonParser.parseSessions(json, "", LocalDate.of(2024, 1, 15)).get(0).cinema(),
            is(equalTo(cinemaName))
        );
    }

    @Test
    void parseSessionsParsesAddress() {
        final String address = "ул. Тестовая " + UUID.randomUUID();
        final String json = buildAfishaJsonWithPlace(
            "Фильм", "2024-01-15T18:00:00", "Кинотеатр", address
        );

        assertThat(
            "cant parse address from JSON",
            SessionJsonParser.parseSessions(json, "", LocalDate.of(2024, 1, 15)).get(0).address(),
            is(equalTo(address))
        );
    }

    @Test
    void parseSessionsParsesPrice() {
        final String json = buildAfishaJson("Фильм", "2024-01-15T18:00:00", "750");

        assertThat(
            "cant parse price from JSON",
            SessionJsonParser.parseSessions(json, "", LocalDate.of(2024, 1, 15)).get(0).price(),
            is(equalTo(750))
        );
    }

    @Test
    void parseSessionsConvertsNullPriceToMinusOne() {
        final String json = buildAfishaJson("Фильм", "2024-01-15T18:00:00", "null");

        assertThat(
            "cant convert null price to -1",
            SessionJsonParser.parseSessions(json, "", LocalDate.of(2024, 1, 15)).get(0).price(),
            is(equalTo(-1))
        );
    }

    @Test
    void parseSessionsDetectsRussianSubtitlesSession() {
        final String json = buildAfishaJsonWithSubtitles(
            "Фильм", "2024-01-15T18:00:00", "russiansubtitlessession"
        );

        assertThat(
            "cant detect Russian subtitles session",
            SessionJsonParser.parseSessions(json, "", LocalDate.of(2024, 1, 15)).get(0).russianSubtitlesSession(),
            is(true)
        );
    }

    @Test
    void parseSessionsDetectsNonSubtitlesSession() {
        final String json = buildAfishaJsonWithSubtitles(
            "Фильм", "2024-01-15T18:00:00", "originalversion"
        );

        assertThat(
            "cant detect non-subtitles session",
            SessionJsonParser.parseSessions(json, "", LocalDate.of(2024, 1, 15)).get(0).russianSubtitlesSession(),
            is(false)
        );
    }

    @Test
    void parseSessionsParsesMultipleSessionsFromSamePlace() {
        final String json = buildAfishaJsonWithMultipleSessions(
            "Фильм",
            List.of("2024-01-15T14:00:00", "2024-01-15T18:00:00", "2024-01-15T21:00:00")
        );
        final List<Session> sessions = SessionJsonParser.parseSessions(json, "", LocalDate.of(2024, 1, 15));

        assertThat(
            "cant parse multiple sessions from same place",
            sessions,
            hasSize(3)
        );
    }

    @Test
    void parseSessionsHandlesMissingDistributorInfo() {
        final String json = buildAfishaJsonWithoutDistributorInfo(
            "Фильм", "2024-01-15T18:00:00"
        );

        assertThat(
            "cant handle missing DistributorInfo and return empty description",
            SessionJsonParser.parseSessions(json, "", LocalDate.of(2024, 1, 15)).get(0).description(),
            is(equalTo(""))
        );
    }

    @Test
    void parseSessionsParsesVerdict() {
        final String verdict = "Отличный фильм " + UUID.randomUUID();
        final String json = buildAfishaJsonWithVerdict(
            "Фильм", "2024-01-15T18:00:00", verdict
        );

        assertThat(
            "cant parse verdict from JSON",
            SessionJsonParser.parseSessions(json, "", LocalDate.of(2024, 1, 15)).get(0).verdict(),
            is(equalTo(verdict))
        );
    }

    @Test
    void parseSessionsReturnsEmptyListWhenSessionDateDoesNotMatchExpectedDate() {
        final String json = buildAfishaJson("Фильм", "2024-01-16T18:00:00", "500");
        final LocalDate expectedDate = LocalDate.of(2024, 1, 15);

        assertThat(
            "cant return empty list when session date does not match expected date",
            SessionJsonParser.parseSessions(json, "", expectedDate),
            is(empty())
        );
    }

    @Test
    void parseSessionsReturnsSessionsWhenDateMatchesExpectedDate() {
        final String json = buildAfishaJson("Фильм", "2024-01-15T18:00:00", "500");
        final LocalDate expectedDate = LocalDate.of(2024, 1, 15);

        assertThat(
            "cant return sessions when date matches expected date",
            SessionJsonParser.parseSessions(json, "", expectedDate),
            hasSize(1)
        );
    }

    private static String buildAfishaJson(
        final String movieName, final String dateTime, final String price
    ) {
        return buildAfishaJsonFull(
            movieName, dateTime, price, List.of("Драма"),
            "Кинотеатр", "Адрес", "Описание", "Вердикт", "standard"
        );
    }

    private static String buildAfishaJsonWithGenres(
        final String movieName, final String dateTime, final List<String> genres
    ) {
        return buildAfishaJsonFull(
            movieName, dateTime, "500", genres,
            "Кинотеатр", "Адрес", "Описание", "Вердикт", "standard"
        );
    }

    private static String buildAfishaJsonWithPlace(
        final String movieName, final String dateTime,
        final String cinemaName, final String address
    ) {
        return buildAfishaJsonFull(
            movieName, dateTime, "500", List.of("Драма"),
            cinemaName, address, "Описание", "Вердикт", "standard"
        );
    }

    private static String buildAfishaJsonWithSubtitles(
        final String movieName, final String dateTime, final String subtitlesFormat
    ) {
        return buildAfishaJsonFull(
            movieName, dateTime, "500", List.of("Драма"),
            "Кинотеатр", "Адрес", "Описание", "Вердикт", subtitlesFormat
        );
    }

    private static String buildAfishaJsonWithVerdict(
        final String movieName, final String dateTime, final String verdict
    ) {
        return buildAfishaJsonFull(
            movieName, dateTime, "500", List.of("Драма"),
            "Кинотеатр", "Адрес", "Описание", verdict, "standard"
        );
    }

    private static String buildAfishaJsonWithMultipleSessions(
        final String movieName, final List<String> dateTimes
    ) {
        final JSONObject root = new JSONObject();
        final JSONObject movieCard = new JSONObject();
        final JSONObject info = buildInfo(movieName, List.of("Драма"), "Описание", "Вердикт");
        movieCard.put("Info", info);
        root.put("MovieCard", movieCard);

        final JSONArray sessions = new JSONArray();
        for (final String dt : dateTimes) {
            sessions.put(buildSession(dt, "500", "standard"));
        }

        final JSONObject place = new JSONObject();
        place.put("Name", "Кинотеатр");
        place.put("Address", "Адрес");

        final JSONObject item = new JSONObject();
        item.put("Place", place);
        item.put("Sessions", sessions);

        final JSONArray items = new JSONArray();
        items.put(item);

        final JSONObject scheduleList = new JSONObject();
        scheduleList.put("Items", items);

        final JSONObject scheduleWidget = new JSONObject();
        scheduleWidget.put("ScheduleList", scheduleList);

        root.put("ScheduleWidget", scheduleWidget);
        return root.toString();
    }

    private static String buildAfishaJsonWithoutDistributorInfo(
        final String movieName, final String dateTime
    ) {
        final JSONObject root = new JSONObject();
        final JSONObject movieCard = new JSONObject();
        final JSONObject info = new JSONObject();
        info.put("Name", movieName);
        info.put("Verdict", "Вердикт");

        final JSONArray genreLinks = new JSONArray();
        genreLinks.put(new JSONObject().put("Name", "Драма"));
        info.put("Genres", new JSONObject().put("Links", genreLinks));

        movieCard.put("Info", info);
        root.put("MovieCard", movieCard);

        final JSONObject session = buildSession(dateTime, "500", "standard");
        final JSONArray sessions = new JSONArray();
        sessions.put(session);

        final JSONObject place = new JSONObject();
        place.put("Name", "Кинотеатр");
        place.put("Address", "Адрес");

        final JSONObject item = new JSONObject();
        item.put("Place", place);
        item.put("Sessions", sessions);

        final JSONArray items = new JSONArray();
        items.put(item);

        final JSONObject scheduleList = new JSONObject();
        scheduleList.put("Items", items);

        final JSONObject scheduleWidget = new JSONObject();
        scheduleWidget.put("ScheduleList", scheduleList);

        root.put("ScheduleWidget", scheduleWidget);
        return root.toString();
    }

    private static String buildAfishaJsonFull(
        final String movieName, final String dateTime, final String price,
        final List<String> genres, final String cinemaName, final String address,
        final String description, final String verdict, final String subtitlesFormat
    ) {
        final JSONObject root = new JSONObject();
        final JSONObject movieCard = new JSONObject();
        final JSONObject info = buildInfo(movieName, genres, description, verdict);
        movieCard.put("Info", info);
        root.put("MovieCard", movieCard);

        final JSONObject session = buildSession(dateTime, price, subtitlesFormat);
        final JSONArray sessions = new JSONArray();
        sessions.put(session);

        final JSONObject place = new JSONObject();
        place.put("Name", cinemaName);
        place.put("Address", address);

        final JSONObject item = new JSONObject();
        item.put("Place", place);
        item.put("Sessions", sessions);

        final JSONArray items = new JSONArray();
        items.put(item);

        final JSONObject scheduleList = new JSONObject();
        scheduleList.put("Items", items);

        final JSONObject scheduleWidget = new JSONObject();
        scheduleWidget.put("ScheduleList", scheduleList);

        root.put("ScheduleWidget", scheduleWidget);
        return root.toString();
    }

    private static JSONObject buildInfo(
        final String movieName, final List<String> genres,
        final String description, final String verdict
    ) {
        final JSONObject info = new JSONObject();
        info.put("Name", movieName);
        info.put("Verdict", verdict);

        final JSONArray genreLinks = new JSONArray();
        for (final String genre : genres) {
            genreLinks.put(new JSONObject().put("Name", genre));
        }
        info.put("Genres", new JSONObject().put("Links", genreLinks));

        final JSONObject distributorInfo = new JSONObject();
        distributorInfo.put("Text", description);
        info.put("DistributorInfo", distributorInfo);

        return info;
    }

    private static JSONObject buildSession(
        final String dateTime, final String price, final String subtitlesFormat
    ) {
        final JSONObject session = new JSONObject();
        session.put("DateTime", dateTime);
        if ("null".equals(price)) {
            session.put("MinPriceFormatted", JSONObject.NULL);
        } else {
            session.put("MinPriceFormatted", price);
        }
        session.put("SubtitlesFormats", subtitlesFormat);
        return session;
    }
}
