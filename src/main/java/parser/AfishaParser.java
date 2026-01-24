package parser;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** Afisha.ru parser. */
public class AfishaParser {
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
            System.out.println("Initial cookies: " + cookies);
        } catch (IOException e) {
            System.err.println("Failed to get initial cookies: " + e.getMessage());
            throw e;
        }
        return cookies;
    }

    /**
     * Parse today films.
     *
     * @return The map of film names to the link to the sessions
     */
    public Map<String, String> parseTodayFilms() throws IOException {
        final Map<String, String> films = new TreeMap<>();
        Map<String, String> pageFilms;
        Set<String> prevFilms = new HashSet<>();
        int page = 0;
        do {
            try {
                pageFilms = parseFilmsPage(String.format(this.filmsPageN, TODAY, page));
                final Set<String> keySet = pageFilms.keySet();
                if (prevFilms.equals(keySet)) {
                    break;
                }
                prevFilms = keySet;
                page++;
                films.putAll(pageFilms);
            } catch (final HttpStatusException httpEx) {
                break;
            }
        } while (!pageFilms.isEmpty());
        return films;
    }

    /**
     * Parse films by provided dates.
     *
     * @return The map of film names to the link to the sessions
     */
    public Map<String, String> parseFilmsInDates(final String dates) throws IOException {
        final Map<String, String> films = new TreeMap<>();
        Map<String, String> pageFilms;
        Set<String> prevFilms = new HashSet<>();
        int page = 0;
        do {
            try {
                pageFilms = parseFilmsPage(String.format(this.filmsPageN, dates, page));
                final Set<String> keySet = pageFilms.keySet();
                if (prevFilms.equals(keySet)) {
                    break;
                }
                prevFilms = keySet;
                page++;
                films.putAll(pageFilms);
            } catch (final HttpStatusException httpEx) {
                break;
            }
        } while (!pageFilms.isEmpty());
        return films;
    }

    /**
     * Parse today films page.
     *
     * @return The map of film names to the link to the sessions
     */
    private static Map<String, String> parseFilmsPage(final String link) throws IOException {
        final Map<String, String> schedules = new HashMap<>();
        final Document document = Jsoup.connect(link).userAgent(USER_AGENT).get();
        final List<Element> filmContainers = document.select("div[data-test='ITEM']");
        for (final Element container : filmContainers) {
            List<Element> linkElements = container.getElementsByAttributeValue(
                "data-test", "LINK LINK-BUTTON TICKET-BUTTON"
            );
            if (linkElements.isEmpty()) {
                linkElements = container.getElementsByAttributeValue("data-test", "LINK LINK-BUTTON");
            }
            if (!linkElements.isEmpty()) {
                final String refElement = linkElements.getFirst().toString();
                final Matcher matcher = FILM_REF_PATTERN.matcher(refElement);
                if (matcher.find()) {
                    final String href = matcher.group(1);
                    if (href.contains("/schedule_cinema_product/")) {
                        schedules.put(
                            container.getElementsByAttributeValue("data-test", "LINK ITEM-NAME ITEM-URL").text(),
                            BASE_LINK + href
                        );
                    } else {
                        // TODO:: Remove later
                        System.out.println("Not cinema product: " + href);
                    }
                } else {
                    throw new IllegalArgumentException("Film ref not found in: " + refElement);
                }
            }
        }
        return schedules;
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
    public List<Session> parseSchedule(final String link, final String date) throws IOException {
        final List<Session> result = new ArrayList<>();
        int page = 1;
        String jsonPage;
        Set<String> prevCinemas = new HashSet<>();
        do {
            try {
                jsonPage = parseSchedulePage(String.format(SCHEDULE_PAGE, link, date, page));
                final List<Session> sessions = getSessions(jsonPage);
                final Set<String> cinemas = sessions.stream().map(Session::cinema).collect(Collectors.toSet());
                if (cinemas.equals(prevCinemas)) {
                    break;
                }
                result.addAll(sessions);
                prevCinemas = cinemas;
                page++;
            } catch (final HttpStatusException httpEx) {
                break;
            }
        } while (!jsonPage.isEmpty());
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
            .timeout(15_000)
            .execute()
            .body();
    }

    private static List<Session> getSessions(final String json) {
        if (json.isEmpty()) {
            return Collections.emptyList();
        }
        final List<Session> result = new ArrayList<>();
        final JSONObject root = new JSONObject(json);
        final JSONObject info = root.getJSONObject("MovieCard").getJSONObject("Info");

        final List<String> genres = extractGenres(info);
        final JSONObject distributorInfo = extractDistributorInfo(info);

        final JSONArray items = root
            .getJSONObject("ScheduleWidget")
            .getJSONObject("ScheduleList")
            .getJSONArray("Items");

        for (int i = 0; i < items.length(); i++) {
            final JSONObject item = items.getJSONObject(i);
            final JSONObject place = item.getJSONObject("Place");
            final JSONArray sessions = item.getJSONArray("Sessions");
            result.addAll(createSessionsForItem(sessions, info, distributorInfo, genres, place));
        }
        return result;
    }

    private static List<String> extractGenres(final JSONObject info) {
        final JSONArray genresArray = info.getJSONObject("Genres").getJSONArray("Links");
        final List<String> genres = new ArrayList<>();
        for (int i = 0; i < genresArray.length(); i++) {
            genres.add(((JSONObject) genresArray.get(i)).get("Name").toString());
        }
        return genres;
    }

    private static JSONObject extractDistributorInfo(final JSONObject info) {
        try {
            return info.getJSONObject("DistributorInfo");
        } catch (final JSONException ignored) {
            return null;
        }
    }

    private static List<Session> createSessionsForItem(
        final JSONArray sessions,
        final JSONObject info,
        final JSONObject distributorInfo,
        final List<String> genres,
        final JSONObject place
    ) {
        final List<Session> result = new ArrayList<>();
        for (int j = 0; j < sessions.length(); j++) {
            final JSONObject session = sessions.getJSONObject(j);
            final String price = session.get("MinPriceFormatted").toString();
            result.add(new Session(
                LocalDateTime.parse(session.get("DateTime").toString()),
                info.get("Name").toString(),
                distributorInfo == null ? "" : distributorInfo.get("Text").toString(),
                info.get("Verdict").toString(),
                genres,
                place.getString("Name"),
                place.get("Address").toString(),
                "null".equals(price) ? -1 : Integer.parseInt(price),
                "link",
                "russiansubtitlessession".equals(session.get("SubtitlesFormats").toString())
            ));
        }
        return result;
    }
}
