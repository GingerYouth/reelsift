package scheduler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import filters.DateInterval;
import java.time.LocalDate;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ThreeWeekInterval}.
 */
final class ThreeWeekIntervalTest {

    @Test
    void intervalStartsOnProvidedDate() {
        final LocalDate today = randomDate();
        final ThreeWeekInterval factory = new ThreeWeekInterval(today);
        final DateInterval interval = factory.create();

        assertThat(
            "interval start date doesnt match provided date",
            interval.start(),
            is(equalTo(today))
        );
    }

    @Test
    void intervalEndsTwentyDaysAfterStart() {
        final LocalDate today = randomDate();
        final ThreeWeekInterval factory = new ThreeWeekInterval(today);
        final DateInterval interval = factory.create();

        assertThat(
            "interval end date doesnt match expected 20 days after start",
            interval.end(),
            is(equalTo(today.plusDays(20)))
        );
    }

    @Test
    void intervalSpansTwentyOneDays() {
        final LocalDate today = randomDate();
        final ThreeWeekInterval factory = new ThreeWeekInterval(today);
        final DateInterval interval = factory.create();

        assertThat(
            "interval doesnt span 21 days",
            interval.getDatesInRange().size(),
            is(equalTo(21))
        );
    }

    @Test
    void intervalContainsStartDate() {
        final LocalDate today = randomDate();
        final ThreeWeekInterval factory = new ThreeWeekInterval(today);
        final DateInterval interval = factory.create();

        assertThat(
            "interval doesnt contain start date",
            interval.getDatesInRange().contains(today),
            is(true)
        );
    }

    @Test
    void intervalContainsEndDate() {
        final LocalDate today = randomDate();
        final ThreeWeekInterval factory = new ThreeWeekInterval(today);
        final DateInterval interval = factory.create();

        assertThat(
            "interval doesnt contain end date",
            interval.getDatesInRange().contains(today.plusDays(20)),
            is(true)
        );
    }

    private static LocalDate randomDate() {
        final int randomDays = new Random().nextInt(365);
        return LocalDate.of(2025, 1, 1).plusDays(randomDays);
    }
}
