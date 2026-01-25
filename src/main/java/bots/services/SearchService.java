package bots.services;

import bots.enums.Guide;
import cache.RedisCache;
import filters.DateInterval;
import filters.Filters;
import filters.Genre;
import filters.LlmFilter;
import filters.MandatoryGenres;
import filters.TimeFilter;
import parser.AfishaParser;
import parser.Session;
import utils.Utils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("PMD.TooManyMethods")
public class SearchService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM");
    private final RedisCache redisCache;
    private final MessageSender messageSender;
    private final UserService userService;

    public SearchService(
        final RedisCache redisCache,
        final MessageSender messageSender,
        final UserService userService
    ) {
        this.redisCache = redisCache;
        this.messageSender = messageSender;
        this.userService = userService;
    }

    public void performSearch(final String chatIdString, final long chatId) throws IOException {
        this.messageSender.sendMessage(chatIdString, Guide.SEARCH.getName());
        
        final Filters filters = buildFilters(chatId);
        final DateInterval dateInterval = getRequiredDateInterval(chatId);
        
        this.cacheMissingData(chatId, dateInterval);
        
        final List<Session> resultSessions = this.redisCache.getCachedSessions(
            dateInterval.getDatesInRange(),
            this.userService.getUserCity(chatId)
        );
        final List<Session> filtered = filters.filter(resultSessions);

        this.messageSender.sendMessage(chatIdString, String.format("\uD83C\uDFAC Найдено %s сеансов!", filtered.size()));

        for (final String split : Session.toSplitStrings(filtered)) {
            this.messageSender.sendMessage(chatIdString, split);
        }
    }

    private Filters buildFilters(final long chatId) {
        final Filters filters = new Filters();
        
        addTimeFilter(filters, chatId);
        addMandatoryGenresFilter(filters, chatId);
        addExcludedGenresFilter(filters, chatId);
        addAiFilter(filters, chatId);
        
        return filters;
    }

    private void addTimeFilter(final Filters filters, final long chatId) {
        final String timeFilter = userService.getTimeFilter(chatId);
        if (timeFilter != null) {
            filters.addFilter(
                new TimeFilter(
                    timeFilter.split("-")[0],
                    timeFilter.split("-")[1]
                )
            );
        }
    }

    private void addMandatoryGenresFilter(final Filters filters, final long chatId) {
        final Set<Genre> mandatoryGenres = userService.getMandatoryGenres(chatId);
        if (mandatoryGenres != null) {
            filters.addFilter(
                new MandatoryGenres(
                    mandatoryGenres.stream()
                        .map(Genre::getDisplayName)
                        .collect(Collectors.toList())
                )
            );
        }
    }

    private void addExcludedGenresFilter(final Filters filters, final long chatId) {
        final Set<Genre> excludedGenres = userService.getExcludedGenres(chatId);
        if (excludedGenres != null) {
            filters.addFilter(
                new MandatoryGenres(
                    excludedGenres.stream()
                        .map(Genre::getDisplayName)
                        .collect(Collectors.toList())
                )
            );
        }
    }

    private void addAiFilter(final Filters filters, final long chatId) {
        final String aiPrompt = userService.getAiPrompt(chatId);
        if (aiPrompt != null) {
            final LlmFilter llmFilter = new LlmFilter(aiPrompt);
            filters.addFilter(llmFilter);
        }
    }

    private DateInterval getRequiredDateInterval(final long chatId) {
        final DateInterval dateInterval = userService.getDateFilter(chatId);
        return dateInterval != null
            ? dateInterval
            : new DateInterval(LocalDate.now(), LocalDate.now());
    }

    private void cacheMissingData(final long chatId, final DateInterval dateInterval) throws IOException {
        final List<LocalDate> requiredDates = dateInterval.getDatesInRange();
        final List<LocalDate> cachedDates = this.redisCache.getCachedDates(this.userService.getUserCity(chatId));
        final List<LocalDate> missingDates = requiredDates.stream()
            .filter(d -> !cachedDates.contains(d))
            .toList();

        if (missingDates.isEmpty()) {
            return;
        }

        final AfishaParser parser = new AfishaParser(userService.getUserCity(chatId));
        final DateTimeFormatter scheduleDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        
        final List<String> dateRanges = convertToDateRanges(missingDates);
        
        for (final String dateRange : dateRanges) {
            final Map<String, String> missingMap = parser.parseFilmsInDates(dateRange);
            final String[] rangeDates = dateRange.split("_");
            final LocalDate startDate = parseSingleDate(rangeDates[0]);
            final LocalDate endDate = parseSingleDate(rangeDates[1]);
            
            for (final Map.Entry<String, String> entry : missingMap.entrySet()) {
                for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                    if (missingDates.contains(date)) {
                        final String formattedDate = date.format(scheduleDateFormatter);
                        final List<Session> missingSessions = parser.parseSchedule(
                            Utils.removeDateFromUrlEnd(entry.getValue()), formattedDate
                        );
                        this.redisCache.cacheSessions(
                            missingSessions,
                            this.userService.getUserCity(chatId)
                        );
                    }
                }
            }
        }
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
                // Gap found, close current range
                ranges.add(formatDateRange(rangeStart, previousDate));
                rangeStart = currentDate;
            }
            previousDate = currentDate;
        }
        
        // Add the final range
        ranges.add(formatDateRange(rangeStart, previousDate));
        
        return ranges;
    }
    
    private String formatDateRange(final LocalDate start, final LocalDate end) {
        final String startDate = start.format(DATE_FORMATTER);
        final String endDate = end.format(DATE_FORMATTER);
        return startDate + "_" + endDate;
    }

    private static LocalDate parseSingleDate(final String dateStr) {
        final String[] dateParts = dateStr.split("-");
        final int day = Integer.parseInt(dateParts[0]);
        final int month = Integer.parseInt(dateParts[1]);
        final int year = Year.now().getValue();
        return LocalDate.of(year, month, day);
    }
}