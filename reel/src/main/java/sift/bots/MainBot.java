package main.java.sift.bots;

import main.java.sift.PropertiesLoader;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/** Main class for bot startup. */
public class MainBot {
    public static void main(String[] args) {
        try {
            final TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(PropertiesLoader.get("tgApiKey"), new SiftBot());
        } catch (TelegramApiException tgApiEx) {
            tgApiEx.printStackTrace();
        }
    }
}
