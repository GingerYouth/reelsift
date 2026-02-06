package bots.services;

import bots.enums.Guide;
import cache.RedisCache;
import filters.DateInterval;
import filters.Filters;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import parser.Session;

/**
 * Service for performing movie session searches.
 * Sends results to Telegram with inline buttons for films
 * that have too many sessions to display inline.
 */
public final class SearchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchService.class);
    private static final int MAX_CAPTION_LENGTH = 1024;
    private static final String TRUNCATION_SUFFIX = " ...";
    private static final int SESSION_THRESHOLD = 10;

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
        this.filterBuilder = new FilterBuilder(userService, messageSender);
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
                : Session.toSplitStrings(filtered, SESSION_THRESHOLD)
        ) {
            final String caption = truncateCaption(film.text());
            sendWithInlineButton(chatIdString, film, caption);
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
        final String chatIdString, final Session.FilmMessage film, final String caption
    ) {
        final String callbackId = this.callbackStore.store(
            0, chatIdString, film.omittedSessions()
        );
        final InlineKeyboardButton button = new InlineKeyboardButton(
            "\uD83C\uDFAC Показать сеансы"
        );
        button.setCallbackData(callbackId);
        final InlineKeyboardRow row = new InlineKeyboardRow(button);
        final InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup(List.of(row));

        if (film.imageUrl() != null && !film.imageUrl().isEmpty()) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Preparing to send photo for chat {} with URL: {} and caption length: {}",
                    chatIdString, film.imageUrl(), caption.length());
            }
            final SendPhoto sendPhoto = SendPhoto.builder()
                .chatId(chatIdString)
                .photo(new InputFile(film.imageUrl()))
                .caption(caption)
                .parseMode(MessageSender.HTML)
                .replyMarkup(keyboardMarkup) // Attach keyboard to photo
                .build();
            try {
                this.messageSender.getTelegramClient().execute(sendPhoto);
            } catch (final TelegramApiException e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Failed to send photo with inline button for URL {}", film.imageUrl());
                }
                // Fallback to sending just the message with button if photo sending fails
                final SendMessage message = new SendMessage(chatIdString, caption);
                message.setReplyMarkup(keyboardMarkup);
                this.messageSender.sendAndGetId(message);
            }
        } else {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Preparing to send text message for chat {} with caption length: {}",
                    chatIdString, caption.length());
            }
            final SendMessage message = new SendMessage(
                chatIdString, caption
            );
            message.setReplyMarkup(keyboardMarkup);
            this.messageSender.sendAndGetId(message);
        }
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
    
    private String truncateCaption(final String text) {
        if (text == null || text.length() <= MAX_CAPTION_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_CAPTION_LENGTH - TRUNCATION_SUFFIX.length()) + TRUNCATION_SUFFIX;
    }
}
