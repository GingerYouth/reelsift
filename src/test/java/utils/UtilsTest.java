package utils;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for {@link Utils}.
 */
final class UtilsTest {

    @Test
    void removeDateAndTrailingSlash() {
        assertThat(
            "cant remove date pattern with trailing slash from URL",
            Utils.removeDateFromUrlEnd("https://www.afisha.ru/msk/cinema/film/12345/21-01-2024/"),
            is(equalTo("https://www.afisha.ru/msk/cinema/film/12345"))
        );
    }

    @Test
    void removeDate() {
        assertThat(
            "cant remove date pattern without trailing slash from URL",
            Utils.removeDateFromUrlEnd("https://www.afisha.ru/msk/cinema/film/12345/21-01-2024"),
            is(equalTo("https://www.afisha.ru/msk/cinema/film/12345"))
        );
    }

    @Test
    void removeDateAndMore() {
        String url = "https://www.afisha.ru/msk/schedule_cinema_product/cheburashka-2-295278/09-02-2026#rcmrclid=c3b6668db2f729b0/10-02-2026/page1/";
        String expected = "https://www.afisha.ru/msk/schedule_cinema_product/cheburashka-2-295278";
        assertThat(
            "should remove date and everything after",
            Utils.removeDateFromUrlEnd(url),
            is(equalTo(expected))
        );
    }

    @Test
    void removeDateDoesNothingForUrlWithoutDate() {
        String url = "https://www.afisha.ru/msk/cinema/film/12345/";
        assertThat(
            "should not change a url that does not contain a date pattern",
            Utils.removeDateFromUrlEnd(url),
            is(equalTo(url))
        );
    }
}