package bots.services;

import bots.enums.Guide;
import cache.RedisCache;
import filters.DateInterval;
import filters.Filters;
import parser.Session;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/** Service for performing movie session searches. */
public class SearchService {

    private final SessionCacheManager cacheManager;
    private final FilterBuilder filterBuilder;
    private final MessageSender messageSender;
    private final UserService userService;

    public SearchService(
        final RedisCache redisCache,
        final MessageSender messageSender,
        final UserService userService
    ) {
        this.cacheManager = new SessionCacheManager(redisCache);
        this.filterBuilder = new FilterBuilder(userService);
        this.messageSender = messageSender;
        this.userService = userService;
    }

    /**
     * Perform a search for movie sessions based on user filters.
     *
     * @param chatIdString The chat ID as string for messaging
     * @param chatId The chat ID for user preferences
     * @throws IOException If parsing or caching fails
     */
    public void performSearch(final String chatIdString, final long chatId) throws IOException {
        this.messageSender.sendMessage(chatIdString, Guide.SEARCH.getName());

        final Filters filters = this.filterBuilder.buildFilters(chatId);
        final DateInterval dateInterval = getDateInterval(chatId);

        this.cacheManager.ensureCached(dateInterval, this.userService.getUserCity(chatId));

        final List<Session> sessions = this.cacheManager.getCachedSessions(
            dateInterval.getDatesInRange(),
            this.userService.getUserCity(chatId)
        );
        final List<Session> filtered = filters.filter(sessions);

        this.messageSender.sendMessage(
            chatIdString,
            String.format("\uD83C\uDFAC Найдено %s сеансов!", filtered.size())
        );

        for (final String part : Session.toSplitStrings(filtered)) {
            this.messageSender.sendMessage(chatIdString, part);
        }
    }

    private DateInterval getDateInterval(final long chatId) {
        final DateInterval dateInterval = this.userService.getDateFilter(chatId);
        if (dateInterval != null) {
            return dateInterval;
        }
        return new DateInterval(LocalDate.now(), LocalDate.now());
    }
}
