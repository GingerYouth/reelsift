package parser;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.json.JSONException;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Afisha.ru parser. */
public class AfishaParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(AfishaParser.class);
    private static final String DATA_TEST_ATTR = "data-test";
    private final Map<String, String> cookies;
    private static final String BASE_LINK = "https://www.afisha.ru";
    private static final String TODAY = "na-segodnya";
    public static final String SCHEDULE_PAGE = "%s/%s/page%d/";

    private final String currentDatePeriod;
    private final String filmsPageN;
    private static final DateTimeFormatter SCHEDULE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final String USER_AGENT =
        "Mozilla/5.0 (Windows NT 11.0; Win64; x64) "
        + "AppleWebKit/537.36 (KHTML, like Gecko) "
        + "Chrome/134.0.6998.166 Safari/537.36";

    private static final Pattern FILM_REF_PATTERN = Pattern.compile(
        "href\\s*=\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE
    );

    private static final int MIN_DELAY_MS = 1000;
    private static final int MAX_DELAY_MS = 3000;
    private static final Random RANDOM = new Random();

    public AfishaParser(final City city) throws IOException {
        this.filmsPageN = "https://www.afisha.ru/" + city.asCode() + "/schedule_cinema/%s/page%d/";
        this.cookies = this.getCookies();
        this.currentDatePeriod = LocalDate.now().format(SCHEDULE_DATE_FORMATTER);
    }

    private Map<String, String> getCookies() throws IOException {
        final Map<String, String> cookies;
        try {
            final Connection.Response initialResponse = Jsoup.connect(BASE_LINK)
                .method(Connection.Method.GET)
                .userAgent(USER_AGENT)
                .execute();
            cookies = initialResponse.cookies();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Initial cookies: {}", cookies);
            }
        } catch (IOException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Failed to get initial cookies: {}", e.getMessage());
            }
            throw e;
        }
        return cookies;
    }

    /**
     * Parse today films.
     *
     * @return The map of film names to the link to the sessions
     * @deprecated Use {@link AfishaParser#parseFilmsInDates(String)} instead
     */
    @Deprecated
    public List<MovieThumbnail> parseTodayFilms() throws IOException {
        final List<MovieThumbnail> films = new ArrayList<>();
        List<MovieThumbnail> pageFilms;
        Set<String> prevNames = new HashSet<>();
        int page = 0;
        do {
            try {
                final String url = String.format(this.filmsPageN, TODAY, page);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Parsing films from: {}", url);
                }
                pageFilms = parseFilmsPage(url);
                final Set<String> names = pageFilms
                    .stream()
                    .map(MovieThumbnail::name)
                    .collect(Collectors.toSet());
                if (prevNames.equals(names)) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Found duplicate film page, stopping parse.");
                    }
                    break;
                }
                prevNames = names;
                page++;
                films.addAll(pageFilms);
                addRandomDelay();
            } catch (final HttpStatusException httpEx) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("HTTP error fetching film page: {}", httpEx.getMessage());
                }
                break;
            }
        } while (!pageFilms.isEmpty());
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Parsed {} films for today.", films.size());
        }
        return films;
    }

    /**
     * Parse films by provided dates.
     *
     * @return The map of film names to the link to the sessions
     */
    public List<MovieThumbnail> parseFilmsInDates(final String dates) throws IOException {
        final List<MovieThumbnail> films = new ArrayList<>();
        List<MovieThumbnail> pageFilms;
        int page = 0;
        do {
            try {
                final String url = String.format(this.filmsPageN, dates, page);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Parsing films from: {}", url);
                }
                pageFilms = parseFilmsPage(url);
                page++;
                films.addAll(pageFilms);
                addRandomDelay();
            } catch (final HttpStatusException httpEx) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("HTTP error fetching film page: {}", httpEx.getMessage());
                }
                break;
            }
        } while (!pageFilms.isEmpty());
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Parsed {} films for dates {}.", films.size(), dates);
        }
        return films;
    }

    /**
     * Parse today films page.
     *
     * @return The list of {@link MovieThumbnail}.
     */
    private static List<MovieThumbnail> parseFilmsPage(final String link) throws IOException {
        final List<MovieThumbnail> thumbnails = new ArrayList<>();
        final Document document = Jsoup.connect(link).userAgent(USER_AGENT).get();
        final List<Element> filmContainers = document.select("div[data-test='ITEM']");

        for (final Element container : filmContainers) {
            extractFilmFromContainer(container)
                .ifPresent(film -> thumbnails.add(
                    new MovieThumbnail(
                        film.getKey(),
                        film.getValue(),
                        extractImageUrl(container)
                    )
                ));
        }
        return thumbnails;
    }

    private static Optional<Map.Entry<String, String>> extractFilmFromContainer(final Element container) {
        List<Element> linkElements = container.getElementsByAttributeValue(
            DATA_TEST_ATTR, "LINK LINK-BUTTON TICKET-BUTTON"
        );
        if (linkElements.isEmpty()) {
            linkElements = container.getElementsByAttributeValue(DATA_TEST_ATTR, "LINK LINK-BUTTON");
        }

        if (!linkElements.isEmpty()) {
            final String refElement = linkElements.getFirst().toString();
            final Matcher matcher = FILM_REF_PATTERN.matcher(refElement);
            if (matcher.find()) {
                final String href = matcher.group(1);
                if (href.contains("/schedule_cinema_product/")) {
                    return Optional.of(Map.entry(
                        container.getElementsByAttributeValue(DATA_TEST_ATTR, "LINK ITEM-NAME ITEM-URL").text(),
                        BASE_LINK + href
                    ));
                } else {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Skipping non-cinema product link: {}", href);
                    }
                }
            } else {
                LOGGER.warn("Film ref not found in element: {}", refElement);
            }
        }
        return Optional.empty();
    }

    private static String extractImageUrl(final Element container) {
        String result = "";
        final List<Element> imgElements = container.getElementsByAttributeValue(DATA_TEST_ATTR, "IMAGE ITEM-IMAGE");
        if (!imgElements.isEmpty()) {
            final String src = imgElements.getFirst().attr("src");
            if (!src.isEmpty()) {
                result = src;
            }
        }
        return result;
    }

    /**
     * Parse specific movie's schedule.
     *
     * @param link The link to the specific movie's schedule
     * @return The list of {@link Session}
     */
    public List<Session> parseSchedule(final String link) throws IOException {
        return parseSchedule(link, this.currentDatePeriod);
    }

    /**
     * Parse specific movie's schedule for a given date.
     *
     * @param link The link to the specific movie's schedule
     * @param date The date in dd-MM-yyyy format
     * @return The list of {@link Session}
     */
    @SuppressWarnings("PMD.CognitiveComplexity")
    public List<Session> parseSchedule(final String link, final String date) throws IOException {
        final List<Session> result = new ArrayList<>();
        int page = 1;
        String jsonPage;
        Set<String> prevCinemas = new HashSet<>();
        do {
            try {
                final String url = String.format(SCHEDULE_PAGE, link, date, page);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Parsing schedule from: {}", url);
                }
                jsonPage = parseSchedulePage(url);
                final List<Session> sessions = SessionJsonParser.parseSessions(jsonPage, url);
                final Set<String> cinemas = sessions.stream().map(Session::cinema).collect(Collectors.toSet());
                if (cinemas.equals(prevCinemas)) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Found duplicate schedule page, stopping parse.");
                    }
                    break;
                }
                result.addAll(sessions);
                prevCinemas = cinemas;
                page++;
                addRandomDelay();
            } catch (final HttpStatusException httpEx) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("HTTP error fetching schedule page: {}", httpEx.getMessage());
                }
                break;
            } catch (final JSONException | IOException e) {
                LOGGER.error("Error parsing schedule for link {} and date {}", link, date, e);
                break;
            }
        } while (!jsonPage.isEmpty());
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Parsed {} sessions for link {} and date {}.", result.size(), link, date);
        }
        return result;
    }

    private String parseSchedulePage(final String page) throws IOException {
        return Jsoup.connect(page)
            .ignoreContentType(true)
            .cookies(this.cookies)
            .header("Accept", "application/json")
            //.header("Referer", apiUrl)
            .header("Sec-Fetch-Dest", "empty")
            .header("Sec-Fetch-Mode", "cors")
            .header("Sec-Fetch-Site", "same-origin")
            .userAgent(USER_AGENT)
            .timeout(30_000)
            .execute()
            .body();
    }

    private static void addRandomDelay() {
        try {
            final long delay = RANDOM.nextInt(MAX_DELAY_MS - MIN_DELAY_MS) + MIN_DELAY_MS;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Adding a delay of {}ms before the next request.", delay);
            }
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Thread interrupted during delay.", e);
        }
    }
}
