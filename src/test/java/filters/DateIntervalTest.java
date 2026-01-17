package filters;

import org.junit.Test;
import static org.junit.Assert.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Unit tests for DateInterval.
 */
public class DateIntervalTest {

    @Test
    public void testValidDateInterval() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 5);
        DateInterval interval = new DateInterval(start, end);

        assertEquals(start, interval.getStart());
        assertEquals(end, interval.getEnd());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidDateInterval() {
        LocalDate start = LocalDate.of(2024, 1, 5);
        LocalDate end = LocalDate.of(2024, 1, 1);
        new DateInterval(start, end); // Should throw exception
    }

    @Test
    public void testGetDatesInRange() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 3);
        DateInterval interval = new DateInterval(start, end);

        List<LocalDate> dates = interval.getDatesInRange();

        assertEquals(3, dates.size());
        assertEquals(LocalDate.of(2024, 1, 1), dates.get(0));
        assertEquals(LocalDate.of(2024, 1, 2), dates.get(1));
        assertEquals(LocalDate.of(2024, 1, 3), dates.get(2));
    }

    @Test
    public void testGetDatesInRangeSingleDay() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 1);
        DateInterval interval = new DateInterval(start, end);

        List<LocalDate> dates = interval.getDatesInRange();

        assertEquals(1, dates.size());
        assertEquals(LocalDate.of(2024, 1, 1), dates.get(0));
    }

    @Test
    public void testToString() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 5);
        DateInterval interval = new DateInterval(start, end);

        assertEquals("2024-01-01-2024-01-05", interval.toString());
    }
}