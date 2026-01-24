package bots.services;

import filters.DateInterval;
import filters.Genre;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class InputValidator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM");

    public static DateInterval validateDateInput(final String input) {
        final String[] parts = input.split("-");
        if (parts.length == 1) {
            final LocalDate date = parseSingleDate(parts[0].trim());
            return new DateInterval(date, date);
        }
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid date format!");
        }
        final LocalDate start = parseSingleDate(parts[0].trim());
        final LocalDate end = parseSingleDate(parts[1].trim());
        return new DateInterval(start, end);
    }

    private static LocalDate parseSingleDate(final String dateStr) {
        final String[] dateParts = dateStr.split("\\.");
        final int day = Integer.parseInt(dateParts[0]);
        final int month = Integer.parseInt(dateParts[1]);
        final int year = Year.now().getValue();
        final LocalDate date = LocalDate.of(year, month, day);
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Invalid date format");
        }
        return date;
    }

    public static String createGenreErrorMessage(final Set<String> invalidGenres) {
        final StringBuilder builder = new StringBuilder(50);
        builder.append("❌ Не распознаны жанры: ")
            .append(String.join(", ", invalidGenres))
            .append("\n\n\uD83D\uDCCB Доступные жанры:\n");

        final List<String> allGenres = new ArrayList<>(Genre.getDisplayNames());
        Collections.sort(allGenres);

        final int mid = (allGenres.size() + 1) / 2;
        for (int i = 0; i < mid; i++) {
            final String left = allGenres.get(i);
            final String right = i + mid < allGenres.size() ? allGenres.get(i + mid) : "";
            builder.append(String.format("%-20s %s%n", left, right));
        }

        return builder.toString();
    }
}