package bots.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Handles all Telegram message sending operations.
 * Centralizes error handling and logging instead of printStackTrace().
 */
public class MessageSender {

    private static final Logger logger = LoggerFactory.getLogger(MessageSender.class);
    private static final String PARSE_MODE = "HTML";

    private final TelegramClient telegramClient;

    public MessageSender(final TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    /**
     * Sends a simple text message.
     *
     * @param chatId the chat ID
     * @param message the message text
     */
    public void send(final String chatId, final String message) {
        if (message == null || message.isEmpty()) {
            logger.warn("Attempted to send empty message to chat {}", chatId);
            return;
        }

        final SendMessage sendMessage = new SendMessage(chatId, message);
        sendMessage.setParseMode(PARSE_MODE);
        send(sendMessage);
    }

    /**
     * Sends a SendMessage object with custom configuration.
     *
     * @param message the SendMessage to send
     */
    public void send(final SendMessage message) {
        if (message == null) {
            logger.warn("Attempted to send null SendMessage");
            return;
        }

        message.setParseMode(PARSE_MODE);
        try {
            this.telegramClient.execute(message);
        } catch (final TelegramApiException exception) {
            logger.error("Failed to send message to chat {}: {}",
                    message.getChatId(),
                    exception.getMessage(),
                    exception);
        }
    }
}
