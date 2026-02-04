package bots.services;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import utils.PropertiesLoader;

/**
 * Sends Telegram messages and handles callback query responses.
 */
@SuppressWarnings("PMD.AvoidPrintStackTrace")
public final class MessageSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageSender.class);
    public static final String HTML = "HTML";

    private final TelegramClient telegramClient;

    /**
     * Creates a sender using the configured Telegram API key.
     */
    public MessageSender() {
        this.telegramClient = new OkHttpTelegramClient(PropertiesLoader.get("tgApiKey"));
    }

    /**
     * Creates a sender with the given Telegram client.
     *
     * @param telegramClient The Telegram client to use
     */
    public MessageSender(final TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    /**
     * Returns the underlying TelegramClient instance.
     * @return The TelegramClient
     */
    public TelegramClient getTelegramClient() {
        return telegramClient;
    }

    /**
     * Sends a text message to the specified chat.
     *
     * @param chatId The chat ID
     * @param message The message text
     */
    public void sendMessage(final String chatId, final String message) {
        final SendMessage sendMessage = new SendMessage(chatId, message);
        sendMessage.setParseMode(HTML);
        try {
            if (!sendMessage.getText().isEmpty()) {
                this.telegramClient.execute(sendMessage);
            }
        } catch (TelegramApiException tgApiEx) {
            tgApiEx.printStackTrace();
        }
    }

    /**
     * Sends a photo with a caption to the specified chat.
     *
     * @param chatId The chat ID
     * @param imageUrl The URL of the image to send
     * @param caption The caption for the image (can be null or empty)
     */
    public void sendPhoto(final String chatId, final String imageUrl, final String caption) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            // If no image URL, fall back to sending just the message
            sendMessage(chatId, caption);
            return;
        }

        LOGGER.info("Attempting to send photo with URL: {}", imageUrl);

        final SendPhoto.SendPhotoBuilder photoBuilder = SendPhoto.builder()
            .chatId(chatId)
            .photo(new InputFile(imageUrl));

        if (caption != null && !caption.trim().isEmpty()) {
            photoBuilder.caption(caption).parseMode(HTML);
        }

        try {
            this.telegramClient.execute(photoBuilder.build());
        } catch (TelegramApiException tgApiEx) {
            LOGGER.error("Failed to send photo for URL {}: {}", imageUrl, tgApiEx.getMessage());
            // Fallback to sending just the message if photo sending fails
            sendMessage(chatId, caption);
        }
    }

    /**
     * Sends a pre-built SendMessage.
     *
     * @param message The SendMessage to send
     */
    public void sendMessage(final SendMessage message) {
        message.setParseMode(HTML);
        try {
            this.telegramClient.execute(message);
        } catch (TelegramApiException tgApiEx) {
            tgApiEx.printStackTrace();
        }
    }

    /**
     * Sends a message and returns the resulting message ID.
     *
     * @param message The SendMessage to send
     * @return The message ID, or empty if sending failed
     */
    public Optional<Integer> sendAndGetId(final SendMessage message) {
        message.setParseMode(HTML);
        try {
            final Message sent = this.telegramClient.execute(message);
            return Optional.of(sent.getMessageId());
        } catch (TelegramApiException tgApiEx) {
            tgApiEx.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Answers a callback query to dismiss the loading indicator.
     *
     * @param callbackQueryId The callback query ID to answer
     */
    public void answerCallback(final String callbackQueryId) {
        final AnswerCallbackQuery answer = new AnswerCallbackQuery(callbackQueryId);
        try {
            this.telegramClient.execute(answer);
        } catch (TelegramApiException tgApiEx) {
            tgApiEx.printStackTrace();
        }
    }
}