package bots;

import bots.services.KeyboardService;
import bots.services.MessageSender;
import bots.services.SearchService;
import bots.services.SessionCallbackStore;
import bots.services.UserService;
import bots.services.handlers.DeleteCommandHandler;
import bots.services.handlers.EditCommandHandler;
import bots.services.handlers.MessageHandler;
import bots.services.handlers.TriggerCommandHandler;
import bots.services.handlers.UserInputHandler;
import cache.RedisCache;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import parser.Session;

/** Telegram bot logic. */
public class SiftBot implements LongPollingSingleThreadUpdateConsumer {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(SiftBot.class);

    private final MessageHandler messageHandler;
    private final UserService userService;
    private final SessionCallbackStore callbackStore;
    private final MessageSender messageSender;

    /**
     * Creates the bot with default Redis and Telegram configuration.
     */
    public SiftBot() {
        this.userService = new UserService();
        this.messageSender = new MessageSender();
        this.callbackStore = new SessionCallbackStore();
        final RedisCache redisCache = new RedisCache("localhost", 6379);
        final KeyboardService keyboardService = new KeyboardService(
            this.messageSender, this.userService
        );
        final SearchService searchService = new SearchService(
            redisCache, this.messageSender,
            this.userService, this.callbackStore
        );
        final TriggerCommandHandler triggerHandler =
            new TriggerCommandHandler(
                this.userService, keyboardService, searchService
            );
        final EditCommandHandler editHandler = new EditCommandHandler(
            this.userService, keyboardService
        );
        final DeleteCommandHandler deleteHandler = new DeleteCommandHandler(
            this.userService, keyboardService
        );
        final UserInputHandler userInputHandler = new UserInputHandler(
            this.userService, keyboardService
        );
        this.messageHandler = new MessageHandler(
            this.userService, keyboardService, triggerHandler,
            editHandler, deleteHandler, userInputHandler
        );
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void consume(final Update update) {
        try {
            if (update.hasCallbackQuery()) {
                this.handleCallback(update.getCallbackQuery());
                return;
            }
            if (!update.hasMessage() || !update.getMessage().hasText()) {
                return;
            }

            final long chatId = update.getMessage().getChatId();
            final String chatIdString = String.valueOf(chatId);
            final String text = update.getMessage().getText();

            LOGGER.debug("Received update from user {}: {}", chatId, text);

            if (
                "спб".equalsIgnoreCase(text)
                    || "мск".equalsIgnoreCase(text)
            ) {
                this.messageHandler.handleCityChange(
                    chatId, chatIdString, text
                );
                return;
            }

            if (!this.isExistingUser(chatId)) {
                this.messageHandler.handleNewUser(chatId, chatIdString);
                return;
            }

            this.messageHandler.processCommand(chatId, chatIdString, text);
        } catch (final Exception ex) {
            LOGGER.error(
                "An unexpected error occurred while processing an update: {}",
                update, ex
            );
        }
    }

    /**
     * Handles an inline button callback query.
     *
     * @param callbackQuery The callback query from Telegram
     */
    private void handleCallback(final CallbackQuery callbackQuery) {
        final String callbackId = callbackQuery.getData();
        final Optional<SessionCallbackStore.StoredCallback> stored =
            this.callbackStore.retrieve(callbackId);
        if (stored.isEmpty()) {
            this.messageSender.answerCallback(callbackQuery.getId());
            return;
        }
        final SessionCallbackStore.StoredCallback callback = stored.get();
        final String formatted = Session.formatSessionLines(
            callback.sessions()
        );
        final SendMessage reply = new SendMessage(
            callback.chatId(), formatted
        );
        reply.setReplyToMessageId(
            callbackQuery.getMessage().getMessageId()
        );
        this.messageSender.sendMessage(reply);
        this.messageSender.answerCallback(callbackQuery.getId());
    }

    /**
     * Checks if the user exists in the system.
     *
     * @param chatId The chat ID
     * @return True if the user exists
     */
    private boolean isExistingUser(final long chatId) {
        return this.userService.hasUserCity(chatId);
    }
}