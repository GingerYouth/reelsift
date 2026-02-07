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
    public static final String SCHEDULE_PAGE = "%s/%s/page%d/";

    private final String currentDatePeriod;
    private final String filmsPageN;
    private final Retrier retrier;
    private static final DateTimeFormatter SCHEDULE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final BrowserProfile browserProfile;

    private static final Pattern FILM_REF_PATTERN = Pattern.compile(
        "href\\s*=\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE
    );

    private static final int MIN_DELAY_MS = 3000;
    private static final int MAX_DELAY_MS = 7000;
    private static final long RETRY_BUDGET_MS = 300_000L;
    private static final long RETRY_INITIAL_DELAY_MS = 5_000L;

    public AfishaParser(final City city) throws IOException {
        this.filmsPageN = "https://www.afisha.ru/" + city.asCode() + "/schedule_cinema/%s/page%d/";
        this.retrier = new Retrier(RETRY_BUDGET_MS, RETRY_INITIAL_DELAY_MS);
        this.browserProfile = BrowserProfile.random();
        this.cookies = this.getCookies();
        this.currentDatePeriod = LocalDate.now().format(SCHEDULE_DATE_FORMATTER);
    }

    @SuppressWarnings("PMD.ExceptionAsFlowControl")
    private Map<String, String> getCookies() throws IOException {
        final Map<String, String> cookies;
        try {
            final Connection.Response initialResponse = this.retrier.execute(
                () -> {
                    try {
                        return browserProfile.applyTo(
                            Jsoup.connect(BASE_LINK)
                                .method(Connection.Method.GET)
                                .timeout(30_000)
                        ).execute();
                    } catch (final HttpStatusException httpEx) {
                        if (isRetryable(httpEx.getStatusCode())) {
                            throw httpEx;
                        }
                        throw new NonRetryableIoException(httpEx);
                    }
                }
            );
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
     * Parse films by provided dates.
     *
     * @return The map of film names to the link to the sessions
     */
    public List<MovieThumbnail> parseFilmsInDates(final String dates) throws IOException {
        final List<MovieThumbnail> films = new ArrayList<>();
        List<MovieThumbnail> pageFilms;
        Set<String> prevNames = new HashSet<>();
        int page = 1;
        do {
            try {
                final String url = String.format(this.filmsPageN, dates, page);
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
                        LOGGER.debug("Found duplicate film page, stopping parse");
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
            LOGGER.info("Parsed {} films for dates {}.", films.size(), dates);
        }
        return films;
    }

    /**
     * Parse today films page.
     *
     * @return The list of {@link MovieThumbnail}.
     */
    private List<MovieThumbnail> parseFilmsPage(final String link) throws IOException {
        final List<MovieThumbnail> thumbnails = new ArrayList<>();
        final Document document = this.retrier.execute(
            () -> {
                try {
                    return browserProfile.applyTo(
                        Jsoup.connect(link)
                            .timeout(30_000)
                    ).get();
                } catch (final HttpStatusException httpEx) {
                    if (isRetryable(httpEx.getStatusCode())) {
                        throw httpEx;
                    }
                    throw new NonRetryableIoException(httpEx);
                }
            }
        );
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
        final LocalDate expectedDate = LocalDate.parse(date, SCHEDULE_DATE_FORMATTER);
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
                final List<Session> sessions = SessionJsonParser.parseSessions(jsonPage, url, expectedDate);
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
        return this.retrier.execute(
            () -> {
                final Connection.Response response;
                try {
                    response = browserProfile.applyTo(
                        Jsoup.connect(page)
                            .ignoreContentType(true)
                            .followRedirects(false)
                            .cookies(cookies)
                            .header("Accept", "application/json")
                            .header("Sec-Fetch-Dest", "empty")
                            .header("Sec-Fetch-Mode", "cors")
                            .header("Sec-Fetch-Site", "same-origin")
                            .timeout(30_000)
                    ).execute();
                } catch (final HttpStatusException httpEx) {
                    if (isRetryable(httpEx.getStatusCode())) {
                        throw httpEx;
                    }
                    throw new NonRetryableIoException(httpEx);
                }
                final int status = response.statusCode();
                if (status >= 300 && status < 400) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(
                            "Schedule page {} redirected with status {}, returning empty",
                            page, status
                        );
                    }
                    return "";
                }
                return response.body();
            }
        );
    }

    /**
     * Checks whether an HTTP status code represents a transient error
     * that should be retried (429 Too Many Requests or server errors 5xx).
     *
     * @param statusCode the HTTP status code
     * @return true if the error is transient and should be retried
     */
    private static boolean isRetryable(final int statusCode) {
        return statusCode == 429 || statusCode >= 500;
    }

    private static void addRandomDelay() {
        try {
            final long delay = new Random().nextInt(MAX_DELAY_MS - MIN_DELAY_MS) + MIN_DELAY_MS;
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
