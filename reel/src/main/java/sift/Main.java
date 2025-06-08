package main.java.sift;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * main.java.sift.Main class.
 */
public class Main {

    private static final String AFISHA_URL =
            "https://www.afisha.ru/msk/schedule_cinema/na-segodnya/";
    private static final ZoneId MOSCOW_ZONE = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    public List<Show> parseShows() throws Exception {
        List<Show> shows = new ArrayList<>();
        Document doc = Jsoup.connect(AFISHA_URL)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .get();

        Elements movieContainers = doc.select("div[data-test='ITEM']");

        for (Element container : movieContainers) {
            if (container.hasAttr("title")) {
                ///msk/schedule_cinema_product/balerina-273955/#rcmrclid=a8df3234cb5b8e38"
                int x = 0;
                Pattern pattern = Pattern.compile(
                    "href\\s*=\\s*\"([^\"]*)\"", // Optimized for double quotes (as in your example)
                    Pattern.CASE_INSENSITIVE
                );

                String link = container
                    .getElementsByAttributeValue("data-test", "LINK LINK-BUTTON TICKET-BUTTON")
                    .get(0)
                    .toString();

                Matcher matcher = pattern.matcher(link);
                if (matcher.find()) {
                    String href = matcher.group(1);
                    System.out.println(href); // Output: /msk/schedule_cinema_product/balerina-273955/#rcmrclid=fed21091bb6a0057
                }

                Show show = new Show(null,
                        container.getElementsByAttribute("title").get(0).text(),
                        null, null);
            }
            Element movieLink = container.selectFirst("a[data-test='event-title-link']");
            String title = movieLink.text();
            URL link = new URL(movieLink.attr("abs:href"));

            Elements timeBlocks = container.select("div[data-test='time-block']");
            for (Element block : timeBlocks) {
                String timeStr = block.selectFirst("span[data-test='time-block-start-time']").text();
                Date showDateTime = parseTime(timeStr);
                shows.add(new Show(showDateTime, title, "", link));
            }
        }
        return shows;
    }

    private Date parseTime(String timeStr) {
        // 1. Get current date in Moscow timezone
        LocalDate today = LocalDate.now(MOSCOW_ZONE);

        // 2. Parse time string (e.g., "13:45")
        LocalTime showTime = LocalTime.parse(timeStr, TIME_FORMATTER);

        // 3. Combine into ZonedDateTime
        ZonedDateTime zonedShowTime = ZonedDateTime.of(today, showTime, MOSCOW_ZONE);

        // 4. Convert to java.util.Date (legacy compatibility)
        return Date.from(zonedShowTime.toInstant());
    }

    /** Main method. */
    public static void main (String[]args) throws Exception {
        List<Show> shows = new Main().parseShows();
        int x = 0;
    }
}