package filters;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an interval between two dates.
 */
public record DateInterval(LocalDate start, LocalDate end) {
    public DateInterval {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("The end date must not be before the start date!");
        }
    }

    public List<LocalDate> getDatesInRange() {
        final List<LocalDate> dates = new ArrayList<>();
        LocalDate current = this.start();
        while (!current.isAfter(this.end())) {
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