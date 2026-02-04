package bots.services;

import cache.RedisCache;
import filters.DateInterval;
import java.util.Optional;
import parser.AfishaParser;
import parser.City;
import parser.MovieThumbnail;
import parser.Session;
import utils.Utils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Manages session caching and retrieval from Redis. */
public class SessionCacheManager {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM");
    private static final DateTimeFormatter SCHEDULE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final RedisCache redisCache;

    public SessionCacheManager(final RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    /**
     * Ensure all required dates are cached for the given city.
     *
     * @param dateInterval The date interval to cache
     * @param city The city to cache sessions for
     * @throws IOException If parsing fails
     */
    public void ensureCached(final DateInterval dateInterval, final City city) throws IOException {
        final List<LocalDate> requiredDates = dateInterval.getDatesInRange();
        final List<LocalDate> cachedDates = this.redisCache.getCachedDates(city);
        final List<LocalDate> missingDates = requiredDates.stream()
            .filter(d -> !cachedDates.contains(d))
            .toList();

        if (missingDates.isEmpty()) {
            return;
        }

        final AfishaParser parser = new AfishaParser(city);
        final List<String> dateRanges = convertToDateRanges(missingDates);

        for (final String dateRange : dateRanges) {
            cacheDateRange(parser, dateRange, missingDates, city);
        }
    }

    /**
     * Get cached sessions for the given dates and city.
     *
     * @param dates The dates to retrieve
     * @param city The city
     * @return List of cached sessions
     */
    public List<Session> getCachedSessions(final List<LocalDate> dates, final City city) {
        return this.redisCache.getCachedSessions(dates, city);
    }

    private void cacheDateRange(
        final AfishaParser parser,
        final String dateRange,
        final List<LocalDate> missingDates,
        final City city
    ) throws IOException {
        final List<MovieThumbnail> thumbnails = parser.parseFilmsInDates(dateRange);
        final String[] rangeDates = dateRange.split("_");
        final LocalDate startDate = parseDateFromRange(rangeDates[0]);
        final LocalDate endDate = parseDateFromRange(rangeDates[1]);

        for (final MovieThumbnail thumbnail : thumbnails) {
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                if (missingDates.contains(date)) {
                    cacheSessionsForDate(
                        parser, thumbnail.sessionsLink(), thumbnail.imageLink(), date, city
                    );
                }
            }
        }
    }

    private void cacheSessionsForDate(
        final AfishaParser parser,
        final String filmUrl,
        final String imageUrl,
        final LocalDate date,
        final City city
    ) throws IOException {
        final String formattedDate = date.format(SCHEDULE_DATE_FORMATTER);
        final List<Session> sessions = parser.parseSchedule(
            Utils.removeDateFromUrlEnd(filmUrl),
            formattedDate
        );
        sessions.forEach(s -> s.setImageUrl(imageUrl));
        this.redisCache.cacheSessions(sessions, city);
    }

    private List<String> convertToDateRanges(final List<LocalDate> dates) {
        if (dates.isEmpty()) {
            return List.of();
        }

        final List<String> ranges = new ArrayList<>();
        LocalDate rangeStart = dates.get(0);
        LocalDate previousDate = rangeStart;

        for (int i = 1; i < dates.size(); i++) {
            final LocalDate currentDate = dates.get(i);
            if (!currentDate.isEqual(previousDate.plusDays(1))) {
                ranges.add(formatDateRange(rangeStart, previousDate));
                rangeStart = currentDate;
            }
            previousDate = currentDate;
        }

        ranges.add(formatDateRange(rangeStart, previousDate));
        return ranges;
    }

    private String formatDateRange(final LocalDate start, final LocalDate end) {
        return start.format(DATE_FORMATTER) + "_" + end.format(DATE_FORMATTER);
    }

    private static LocalDate parseDateFromRange(final String dateStr) {
        final String[] parts = dateStr.split("-");
        final int day = Integer.parseInt(parts[0]);
        final int month = Integer.parseInt(parts[1]);
        return LocalDate.of(Year.now().getValue(), month, day);
    }
}
