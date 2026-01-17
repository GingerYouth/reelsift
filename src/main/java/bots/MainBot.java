package bots;

import utils.PropertiesLoader;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/** parser.Main class for bot startup. */
public class MainBot {
    @SuppressWarnings({"PMD.CloseResource", "PMD.AvoidPrintStackTrace"})
    public static void main(final String[] args) {
        try {
            final TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(PropertiesLoader.get("tgApiKey"), new SiftBot());
        } catch (TelegramApiException tgApiEx) {
            tgApiEx.printStackTrace();
        }
    }
}
