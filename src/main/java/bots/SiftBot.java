package bots;

import bots.command.CommandHandler;
import bots.command.SearchCommandHandler;
import bots.enums.Delete;
import bots.enums.Edit;
import bots.enums.Trigger;
import bots.input.BotInputProcessor;
import bots.messaging.MessageSender;
import bots.state.UserStateManager;
import bots.ui.KeyboardFactory;
import cache.RedisCache;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import parser.City;
import utils.PropertiesLoader;

/**
 * Telegram bot entry point for handling incoming messages.
 * Routes messages to appropriate handlers without managing state directly.
 */
public class SiftBot implements LongPollingSingleThreadUpdateConsumer {

    private final UserStateManager stateManager;
    private final MessageSender messageSender;
    private final KeyboardFactory keyboardFactory;
    private final CommandHandler commandHandler;
    private final BotInputProcessor inputProcessor;

    /**
     * Creates a new SiftBot instance with all required dependencies.
     */
    public SiftBot() {
        final TelegramClient telegramClient = new OkHttpTelegramClient(PropertiesLoader.get("tgApiKey"));
        final RedisCache redisCache = new RedisCache("localhost", 6379);

        this.stateManager = new UserStateManager();
        this.messageSender = new MessageSender(telegramClient);
        this.keyboardFactory = new KeyboardFactory(this.stateManager);
        final SearchCommandHandler searchHandler = new SearchCommandHandler(this.stateManager, this.messageSender, redisCache);
        this.commandHandler = new CommandHandler(this.stateManager, this.messageSender, this.keyboardFactory, searchHandler);
        this.inputProcessor = new BotInputProcessor(this.stateManager, this.messageSender, this.keyboardFactory);
    }


    @Override
    public void consume(final Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        final long chatId = update.getMessage().getChatId();
        final String chatIdString = String.valueOf(chatId);

        // Initialize new users
        if (!this.stateManager.userExists(chatId)) {
            this.stateManager.initializeUser(chatId);
            this.messageSender.send(
                    chatIdString,
                    "👋 Привет! Добро пожаловать в SiftBot.\n"
                            + "По умолчанию установлен город Москва.\n"
                            + "Вы можете изменить город на Питер, написав в чат 'спб'.\n"
            );
            this.inputProcessor.showMainMenu(chatIdString, "Выберите действие:");
            return;
        }

        final String text = update.getMessage().getText();

        // Handle city changes
        if (handleCityChange(chatId, chatIdString, text)) {
            return;
        }

        // Route to command handlers
        routeMessage(chatId, chatIdString, text);
    }

    /**
     * Handles city change commands.
     *
     * @param chatId the chat ID
     * @param chatIdStr the chat ID as string
     * @param text the message text
     * @return true if a city change was handled
     */
    private boolean handleCityChange(final long chatId, final String chatIdStr, final String text) {
        if ("спб".equalsIgnoreCase(text)) {
            this.stateManager.setCity(chatId, City.SPB);
            this.messageSender.send(chatIdStr, "Город изменен на Санкт-Петербург.");
            this.inputProcessor.showMainMenu(chatIdStr, "Выберите действие:");
            return true;
        }

        if ("мск".equalsIgnoreCase(text)) {
            this.stateManager.setCity(chatId, City.MOSCOW);
            this.messageSender.send(chatIdStr, "Город изменен на Москва.");
            this.inputProcessor.showMainMenu(chatIdStr, "Выберите действие:");
            return true;
        }

        return false;
    }

    /**
     * Routes message to appropriate command or input handler.
     *
     * @param chatId the chat ID
     * @param chatIdStr the chat ID as string
     * @param text the message text
     */
    private void routeMessage(final long chatId, final String chatIdStr, final String text) {
        Trigger.getEnumByString(text).ifPresentOrElse(
                trigger -> this.commandHandler.handleTrigger(chatId, chatIdStr, trigger),
                () -> Edit.getEnumByString(text).ifPresentOrElse(
                        edit -> this.commandHandler.handleEdit(chatId, chatIdStr, edit),
                        () -> Delete.getEnumByString(text).ifPresentOrElse(
                                delete -> this.commandHandler.handleDelete(chatId, chatIdStr, delete),
                                () -> this.inputProcessor.processInput(chatId, chatIdStr, text)
                        )
                )
        );
    }
}

