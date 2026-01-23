package bots.validation;

import bots.exception.InvalidDateException;
import filters.Genre;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import filters.DateInterval;

/**
 * Validates and parses user input for various filter types.
 * Replaces raw IllegalArgumentException with custom exceptions.
 */
public class InputValidator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM");

    private InputValidator() {
        // Utility class
    }

    /**
     * Validates and parses date input. Supports single date or date range (dd.MM-dd.MM).
     *
     * @param input the date input string
     * @return parsed DateInterval
     * @throws InvalidDateException if date format is invalid or date is in the past
     */
    public static DateInterval validateDateInput(final String input) throws InvalidDateException {
        final String[] parts = input.split("-");

        if (parts.length == 1) {
            final LocalDate date = parseSingleDate(parts[0].trim());
            return new DateInterval(date, date);
        }

        if (parts.length != 2) {
            throw new InvalidDateException("Date range must be in format: dd.MM or dd.MM-dd.MM");
        }

        final LocalDate start = parseSingleDate(parts[0].trim());
        final LocalDate end = parseSingleDate(parts[1].trim());

        if (start.isAfter(end)) {
            throw new InvalidDateException("Start date cannot be after end date");
        }

        return new DateInterval(start, end);
    }

    /**
     * Parses a single date string in dd.MM format, automatically setting the current year.
     *
     * @param dateStr the date string in dd.MM format
     * @return parsed LocalDate
     * @throws InvalidDateException if date is invalid or in the past
     */
    private static LocalDate parseSingleDate(final String dateStr) throws InvalidDateException {
        try {
            final LocalDate date = MonthDay.parse(dateStr, DATE_FORMATTER)
                    .atYear(Year.now().getValue());

            if (date.isBefore(LocalDate.now())) {
                throw new InvalidDateException("Date cannot be in the past");
            }

            return date;
        } catch (final Exception e) {
            throw new InvalidDateException("Invalid date format. Expected dd.MM format", e);
        }
    }

    /**
     * Validates genre input and returns a result with valid and invalid genres.
     *
     * @param input the genre input string
     * @return GenreValidationResult with separated valid and invalid genres
     */
    public static GenreValidationResult validateGenres(final String input) {
        final Genre.ParseResult parseResult = Genre.parseGenres(input);
        return new GenreValidationResult(
                parseResult.validGenres(),
                parseResult.invalidGenres()
        );
    }

    /**
     * Result object for genre validation.
     */
    public static class GenreValidationResult {
        private final Set<Genre> validGenres;
        private final Set<String> invalidGenres;

        public GenreValidationResult(final Set<Genre> validGenres, final Set<String> invalidGenres) {
            this.validGenres = validGenres;
            this.invalidGenres = invalidGenres;
        }

        public Set<Genre> getValidGenres() {
            return validGenres;
        }

        public Set<String> getInvalidGenres() {
            return invalidGenres;
        }

        public boolean isValid() {
            return invalidGenres.isEmpty();
        }
    }
}
