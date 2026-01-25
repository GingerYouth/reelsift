package bots;

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import utils.PropertiesLoader;

/** parser.Main class for bot startup. */
public class MainBot {
    @SuppressWarnings({"PMD.CloseResource", "PMD.AvoidPrintStackTrace"})
    public static void main(final String[] args) {
        try {
            //new RedisCache("localhost", 6379).invalidateAll();
            final TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(PropertiesLoader.get("tgApiKey"), new SiftBot());
        } catch (TelegramApiException tgApiEx) {
            tgApiEx.printStackTrace();
        }
    }
}
