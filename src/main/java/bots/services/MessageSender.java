package bots.services;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import utils.PropertiesLoader;

public class MessageSender {
    public static final String HTML = "HTML";
    private final TelegramClient telegramClient;

    public MessageSender() {
        this.telegramClient = new OkHttpTelegramClient(PropertiesLoader.get("tgApiKey"));
    }

    public void sendMessage(String chatId, String message) {
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

    public void sendMessage(SendMessage message) {
        message.setParseMode(HTML);
        try {
            this.telegramClient.execute(message);
        } catch (TelegramApiException tgApiEx) {
            tgApiEx.printStackTrace();
        }
    }
}