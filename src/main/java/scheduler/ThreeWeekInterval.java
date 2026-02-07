package scheduler;

import filters.DateInterval;
import java.time.LocalDate;

/**
 * Factory for creating a {@link DateInterval} spanning 3 weeks from today.
 */
public final class ThreeWeekInterval {

    private static final int WEEKS_AHEAD = 3;

    private final LocalDate today;

    /**
     * Creates a factory using the current date.
     */
    public ThreeWeekInterval() {
        this(LocalDate.now());
    }

    /**
     * Creates a factory using the specified date as "today".
     *
     * @param today The date to use as the starting point
     */
    public ThreeWeekInterval(final LocalDate today) {
        this.today = today;
    }

    /**
     * Creates a date interval spanning 3 weeks from today.
     * The interval includes today and ends 20 days later (21 days total).
     *
     * @return A date interval from today to today + 20 days
     */
    public DateInterval create() {
        return new DateInterval(
            this.today,
            this.today.plusWeeks(WEEKS_AHEAD).minusDays(1)
        );
    }
}
