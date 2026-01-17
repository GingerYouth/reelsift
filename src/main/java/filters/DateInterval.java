package filters;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an interval between two dates.
 */
public class DateInterval {
    private final LocalDate start;
    private final LocalDate end;

    public DateInterval(final LocalDate start, final LocalDate end) {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("The end date must not be before the start date!");
        }
        this.start = start;
        this.end = end;
    }

    public LocalDate getStart() {
        return this.start;
    }

    public LocalDate getEnd() {
        return this.end;
    }

    public List<LocalDate> getDatesInRange() {
        final List<LocalDate> dates = new ArrayList<>();
        LocalDate current = this.getStart();
        while (!current.isAfter(this.getEnd())) {
            dates.add(current);
            current = current.plusDays(1);
        }
        return dates;
    }

    @Override
    public String toString() {
        return String.format("%s-%s", this.start, this.end);
    }
}