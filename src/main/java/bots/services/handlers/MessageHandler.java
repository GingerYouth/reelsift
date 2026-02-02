package bots.services.handlers;

import bots.enums.DeleteCommand;
import bots.enums.EditCommand;
import bots.enums.TriggerCommand;
import bots.services.KeyboardService;
import bots.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageHandler.class);
    private final UserService userService;
    private final KeyboardService keyboardService;
    private final TriggerCommandHandler triggerHandler;
    private final EditCommandHandler editHandler;
    private final DeleteCommandHandler deleteHandler;
    private final UserInputHandler userInputHandler;

    public MessageHandler(
        final UserService userService,
        final KeyboardService keyboardService,
        final TriggerCommandHandler triggerHandler,
        final EditCommandHandler editHandler,
        final DeleteCommandHandler deleteHandler,
        final UserInputHandler userInputHandler
    ) {
        this.userService = userService;
        this.keyboardService = keyboardService;
        this.triggerHandler = triggerHandler;
        this.editHandler = editHandler;
        this.deleteHandler = deleteHandler;
        this.userInputHandler = userInputHandler;
    }

    public void handleNewUser(final long chatId, final String chatIdString) {
        LOGGER.info("New user with chat ID: {}", chatId);
        this.userService.setUserCity(chatId, parser.City.MOSCOW);
        this.keyboardService.showMainKeyboard(
            chatIdString,
            "👋 Привет! Добро пожаловать в SiftBot.\n"
                + "По умолчанию установлен город Москва.\n"
                + "Вы можете изменить город на Питер, написав в чат 'спб'.\n"
        );
    }

    public void handleCityChange(final long chatId, final String chatIdString, final String text) {
        if ("спб".equalsIgnoreCase(text)) {
            LOGGER.info("User {} changed city to SPB", chatId);
            userService.setUserCity(chatId, parser.City.SPB);
            keyboardService.showMainKeyboard(chatIdString, "Город изменен на Санкт-Петербург.");
        } else if ("мск".equalsIgnoreCase(text)) {
            LOGGER.info("User {} changed city to MOSCOW", chatId);
            userService.setUserCity(chatId, parser.City.MOSCOW);
            keyboardService.showMainKeyboard(chatIdString, "Город изменен на Москва.");
        } else {
            keyboardService.showMainKeyboard(chatIdString, "Выберите действие:");
        }
    }

    public void processCommand(final long chatId, final String chatIdString, final String text) {
        LOGGER.info("Processing command '{}' for user {}", text, chatId);
        TriggerCommand.getEnumByString(text).ifPresentOrElse(
            triggerCommand -> this.triggerHandler.handle(chatId, chatIdString, triggerCommand),
            () -> EditCommand.getEnumByString(text).ifPresentOrElse(
                editCommand -> this.editHandler.handle(chatId, chatIdString, editCommand),
                () -> DeleteCommand.getEnumByString(text).ifPresentOrElse(
                    deleteCommand -> this.deleteHandler.handle(chatId, chatIdString, deleteCommand),
                    () -> this.userInputHandler.handleUserInput(chatId, chatIdString, text)
                )
            )
        );
    }
}