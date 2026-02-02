package bots;

import bots.services.KeyboardService;
import bots.services.MessageSender;
import bots.services.SearchService;
import bots.services.UserService;
import bots.services.handlers.DeleteCommandHandler;
import bots.services.handlers.EditCommandHandler;
import bots.services.handlers.TriggerCommandHandler;
import bots.services.handlers.MessageHandler;
import bots.services.handlers.UserInputHandler;
import cache.RedisCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

/** Telegram bot logic. */
public class SiftBot implements LongPollingSingleThreadUpdateConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SiftBot.class);
    private final RedisCache redisCache;
    private final MessageHandler messageHandler;
    private final UserService userService;
    
    public SiftBot() {
        this.redisCache = new RedisCache("localhost", 6379);
        
        // Initialize services
        this.userService = new UserService();
        final MessageSender messageSender = new MessageSender();
        final KeyboardService keyboardService = new KeyboardService(messageSender, userService);
        final SearchService searchService = new SearchService(redisCache, messageSender, userService);
        
        // Initialize handlers
        final TriggerCommandHandler triggerHandler = new TriggerCommandHandler(userService, keyboardService, searchService);
        final EditCommandHandler editHandler = new EditCommandHandler(userService, keyboardService);
        final DeleteCommandHandler deleteHandler = new DeleteCommandHandler(this.userService, keyboardService);
        final UserInputHandler userInputHandler = new UserInputHandler(userService, keyboardService);
        
        this.messageHandler = new MessageHandler(
            this.userService, keyboardService, triggerHandler,
                editHandler, deleteHandler, userInputHandler
        );
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void consume(final Update update) {
        try {
            if (!update.hasMessage() || !update.getMessage().hasText()) {
                return;
            }

            final long chatId = update.getMessage().getChatId();
            final String chatIdString = String.valueOf(chatId);
            final String text = update.getMessage().getText();
            
            LOGGER.debug("Received update from user {}: {}", chatId, text);

            // Check if it's a city change command
            if ("спб".equalsIgnoreCase(text) || "мск".equalsIgnoreCase(text)) {
                this.messageHandler.handleCityChange(chatId, chatIdString, text);
                return;
            }

            // Handle new users
            if (!this.isExistingUser(chatId)) {
                this.messageHandler.handleNewUser(chatId, chatIdString);
                return;
            }

            // Process commands and user input
            this.messageHandler.processCommand(chatId, chatIdString, text);
        } catch (final Exception e) {
            LOGGER.error("An unexpected error occurred while processing an update: {}", update, e);
        }
    }
    
    private boolean isExistingUser(final long chatId) {
        return this.userService.hasUserCity(chatId);
    }
}