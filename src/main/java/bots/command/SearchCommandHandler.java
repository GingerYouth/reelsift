package bots.command;

import bots.messaging.MessageSender;
import bots.state.UserStateManager;
import cache.RedisCache;
import filters.DateInterval;
import filters.Filters;
import filters.Genre;
import filters.LlmFilter;
import filters.MandatoryGenres;
import filters.TimeFilter;
import parser.AfishaParser;
import parser.City;
import parser.Session;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles search command execution with filtering logic.
 * Orchestrates search coordination between parser, cache, and filters.
 */
public class SearchCommandHandler {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM");

    private final UserStateManager stateManager;
    private final MessageSender messageSender;
    private final RedisCache redisCache;

    public SearchCommandHandler(
            final UserStateManager stateManager,
            final MessageSender messageSender,
            final RedisCache redisCache) {
        this.stateManager = stateManager;
        this.messageSender = messageSender;
        this.redisCache = redisCache;
    }

    /**
     * Executes a search with current user filters.
     *
     * @param chatId the chat ID
     * @param chatIdStr the chat ID as string
     */
    public void executeSearch(final long chatId, final String chatIdStr) {
        try {
            this.messageSender.send(chatIdStr, "🔍 Поиск фильмов...");

            final Filters filters = buildFilters(chatId);
            final City city = this.stateManager.getCity(chatId);
            final List<LocalDate> requiredDates = getRequiredDates(chatId);

            fetchAndCacheMissingDates(city, requiredDates);

            final List<Session> resultSessions = this.redisCache
                    .getCachedSessions(requiredDates, city);
            final List<Session> filtered = filters.filter(resultSessions);

            this.messageSender.send(chatIdStr, String.format("\uD83C\uDFAC Найдено %s сеансов!", filtered.size()));

            for (final String split : Session.toSplitStrings(filtered)) {
                this.messageSender.send(chatIdStr, split);
            }
        } catch (final Exception exception) {
            this.messageSender.send(chatIdStr, "❌ Ошибка при поиске фильмов. Попробуйте позже.");
        }
    }

    /**
     * Builds filters based on user's saved preferences.
     *
     * @param chatId the chat ID
     * @return configured Filters object
     */
    private Filters buildFilters(final long chatId) {
        final Filters filters = new Filters();

        if (this.stateManager.hasTimeFilter(chatId)) {
            final String[] timeParts = this.stateManager.getTimeFilter(chatId).split("-");
            if (timeParts.length == 2) {
                filters.addFilter(new TimeFilter(timeParts[0], timeParts[1]));
            }
        }

        if (this.stateManager.hasMandatoryGenres(chatId)) {
            filters.addFilter(
                    new MandatoryGenres(
                            this.stateManager.getMandatoryGenres(chatId).stream()
                                    .map(Genre::getDisplayName)
                                    .collect(Collectors.toList())
                    )
            );
        }

        if (this.stateManager.hasExcludedGenres(chatId)) {
            filters.addFilter(
                    new MandatoryGenres(
                            this.stateManager.getExcludedGenres(chatId).stream()
                                    .map(Genre::getDisplayName)
                                    .collect(Collectors.toList())
                    )
            );
        }

        if (this.stateManager.hasAiPrompt(chatId)) {
            final LlmFilter llmFilter = new LlmFilter(this.stateManager.getAiPrompt(chatId));
            filters.addFilter(llmFilter);
        }

        return filters;
    }

    /**
     * Gets required dates for search.
     *
     * @param chatId the chat ID
     * @return list of required dates
     */
    private List<LocalDate> getRequiredDates(final long chatId) {
        if (this.stateManager.hasDateFilter(chatId)) {
            return this.stateManager.getDateFilter(chatId).getDatesInRange();
        }
        return List.of(LocalDate.now());
    }

    /**
     * Fetches and caches missing dates.
     *
     * @param city the city to search in
     * @param requiredDates the dates to fetch
     * @throws Exception if parsing fails
     */
    private void fetchAndCacheMissingDates(final City city, final List<LocalDate> requiredDates) throws Exception {
        final List<LocalDate> cachedDates = this.redisCache.getCachedDates(city);
        final List<LocalDate> missingDates = requiredDates.stream()
                .filter(d -> !cachedDates.contains(d))
                .toList();

        if (missingDates.isEmpty()) {
            return;
        }

        final AfishaParser parser = new AfishaParser(city);
        for (final LocalDate missing : missingDates) {
            fetchAndCacheDate(parser, missing, city);
        }
    }

    /**
     * Fetches and caches sessions for a single date.
     *
     * @param parser the Afisha parser
     * @param date the date to fetch
     * @param city the city
     * @throws Exception if parsing fails
     */
    private void fetchAndCacheDate(final AfishaParser parser, final LocalDate date, final City city) throws Exception {
        final Map<String, String> filmsMap = parser.parseFilmsInDates(date.format(DATE_FORMATTER));
        for (final Map.Entry<String, String> entry : filmsMap.entrySet()) {
            final List<Session> sessions = parser.parseSchedule(entry.getValue());
            this.redisCache.cacheSessions(sessions, city);
        }
    }
}
