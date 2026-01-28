package utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.UUID;

import filters.LlmFilter;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Utils}.
 * Uses a fake DeepSeekService to test filtering logic without network calls.
 */
final class UtilsTest {

    @Test
    void removeDateFromUrlEndRemovesDatePatternAndTrailingSlash() {
        assertThat(
            "cant remove date pattern with trailing slash from URL",
            Utils.removeDateFromUrlEnd("https://www.afisha.ru/msk/cinema/film/12345/21-01-2024/"),
            is(equalTo("https://www.afisha.ru/msk/cinema/film/12345"))
        );
    }

    @Test
    void removeDateFromUrlEndRemovesDatePatternWithoutTrailingSlash() {
        assertThat(
            "cant remove date pattern without trailing slash from URL",
            Utils.removeDateFromUrlEnd("https://www.afisha.ru/msk/cinema/film/12345/21-01-2024"),
            is(equalTo("https://www.afisha.ru/msk/cinema/film/12345"))
        );
    }

    @Test
    void removeDateFromUrlEndHandlesRandomUrlWithDate() {
        final String randomPath = UUID.randomUUID().toString().replace("-", "");
        assertThat(
            "cant remove date from URL with random path segment",
            Utils.removeDateFromUrlEnd("https://example.com/" + randomPath + "/15-06-2025/"),
            is(equalTo("https://example.com/" + randomPath))
        );
    }
}
