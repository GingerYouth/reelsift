package bots.services;

import bots.enums.Guide;
import cache.RedisCache;
import filters.DateInterval;
import filters.Filters;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import parser.Session;

/**
 * Service for performing movie session searches.
 * Sends results to Telegram with inline buttons for films
 * that have too many sessions to display inline.
 */
public final class SearchService {

    private final SessionCacheManager cacheManager;
    private final FilterBuilder filterBuilder;
    private final MessageSender messageSender;
    private final UserService userService;
    private final SessionCallbackStore callbackStore;

    /**
     * Creates a search service with the given dependencies.
     *
     * @param redisCache The Redis cache for session data
     * @param messageSender The message sender for Telegram
     * @param userService The user preference service
     * @param callbackStore The callback store for inline buttons
     */
    public SearchService(
        final RedisCache redisCache,
        final MessageSender messageSender,
        final UserService userService,
        final SessionCallbackStore callbackStore
    ) {
        this.cacheManager = new SessionCacheManager(redisCache);
        this.filterBuilder = new FilterBuilder(userService);
        this.messageSender = messageSender;
        this.userService = userService;
        this.callbackStore = callbackStore;
    }

    /**
     * Perform a search for movie sessions based on user filters.
     *
     * @param chatIdString The chat ID as string for messaging
     * @param chatId The chat ID for user preferences
     * @throws IOException If parsing or caching fails
     */
    public void performSearch(
        final String chatIdString, final long chatId
    ) throws IOException {
        this.messageSender.sendMessage(chatIdString, Guide.SEARCH.getName());

        final Filters filters = this.filterBuilder.buildFilters(chatId);
        final DateInterval dateInterval = getDateInterval(chatId);

        this.cacheManager.ensureCached(
            dateInterval, this.userService.getUserCity(chatId)
        );

        final List<Session> sessions = this.cacheManager.getCachedSessions(
            dateInterval.getDatesInRange(),
            this.userService.getUserCity(chatId)
        );
        final List<Session> filtered = filters.filter(sessions);

        this.messageSender.sendMessage(
            chatIdString,
            String.format(
                "\uD83C\uDFAC Найдено %s сеансов!", filtered.size()
            )
        );

        for (
            final Session.FilmMessage film
                : Session.toSplitStrings(filtered, 10)
        ) {
            if (film.omittedSessions().isEmpty()) {
                this.messageSender.sendMessage(chatIdString, film.text());
            } else {
                sendWithInlineButton(chatIdString, film);
            }
        }
    }

    /**
     * Sends a film message with an inline "Показать сеансы" button.
     *
     * @param chatIdString The chat ID
     * @param film The film message with omitted sessions
     */
    @SuppressWarnings("PMD.LooseCoupling")
    private void sendWithInlineButton(
        final String chatIdString, final Session.FilmMessage film
    ) {
        final String callbackId = this.callbackStore.store(
            0, chatIdString, film.omittedSessions()
        );
        final SendMessage message = new SendMessage(
            chatIdString, film.text()
        );
        final InlineKeyboardButton button = new InlineKeyboardButton(
            "\uD83C\uDFAC Показать сеансы"
        );
        button.setCallbackData(callbackId);
        final InlineKeyboardRow row = new InlineKeyboardRow(button);
        message.setReplyMarkup(new InlineKeyboardMarkup(List.of(row)));
        this.messageSender.sendAndGetId(message);
    }

    /**
     * Gets the date interval from user preferences or defaults to today.
     *
     * @param chatId The chat ID
     * @return The date interval
     */
    private DateInterval getDateInterval(final long chatId) {
        final DateInterval dateInterval =
            this.userService.getDateFilter(chatId);
        if (dateInterval != null) {
            return dateInterval;
        }
        return new DateInterval(LocalDate.now(), LocalDate.now());
    }
}
