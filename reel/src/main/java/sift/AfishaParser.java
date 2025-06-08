package main.java.sift;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Afisha.ru parser.
 */
public class AfishaParser {
    private static final String BASE_LINK = "https://www.afisha.ru";
    private static final String TODAY_FILMS_PAGE_N = "https://www.afisha.ru/msk/schedule_cinema/na-segodnya/page%d/";
    private static final ZoneId MOSCOW_ZONE = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter HH_MM_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final String USER_AGENT =
        "Mozilla/5.0 (Windows NT 11.0; Win64; x64) "
        + "AppleWebKit/537.36 (KHTML, like Gecko) "
        + "Chrome/134.0.6998.166 Safari/537.36";
    private static final Pattern FILM_REF_PATTERN = Pattern.compile(
        "href\\s*=\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE
    );

    /**
     * Parse today films.
     *
     * @return The map of film names to the link to the shows
     */
    public static Map<String, String> parseTodayFilms() throws Exception {
        final Map<String, String> films = new TreeMap<>();
        Map<String, String> pageFilms;
        int page = 0;
        do {
            pageFilms = parseFilmsPage(String.format(TODAY_FILMS_PAGE_N, page));
            page++;
            films.putAll(pageFilms);
        } while (!pageFilms.isEmpty());
        return films;
    }

    /**
     * Parse films page.
     *
     * @return The map of film names to the link to the shows
     */
    private static Map<String, String> parseFilmsPage(final String link) throws Exception {
        final Map<String, String> schedules = new HashMap<>();
        final Document document = Jsoup.connect(link).userAgent(USER_AGENT).get();
        final Elements filmContainers = document.select("div[data-test='ITEM']");
        for (final Element container : filmContainers) {
            Elements linkElements = container.getElementsByAttributeValue(
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
     * Parse schedule films page.
     */
    public static Map<String, String> parseSchedulePage(final String link) throws Exception {
        final Document document = Jsoup
            .connect("https://www.afisha.ru/movie/balerina-273955/08-06-2025/#rcmrclid=8c27769ac5acfa57")
            .userAgent(USER_AGENT).get();
        // TODO:: Implement loading of hidden shows
        return null;
    }
}
