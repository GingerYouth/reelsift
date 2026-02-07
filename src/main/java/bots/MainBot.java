package bots;

import cache.RedisCache;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import scheduler.SchedulerInitializer;
import utils.PropertiesLoader;

/**
 * Main class for bot startup.
 * Initializes Redis cache, scheduler, and Telegram bot.
 */
public class MainBot {

    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;

    /**
     * Application entry point.
     *
     * @param args Command line arguments (unused)
     */
    @SuppressWarnings({"PMD.CloseResource", "PMD.AvoidPrintStackTrace"})
    public static void main(final String[] args) {
        try {
            final RedisCache redisCache = new RedisCache(REDIS_HOST, REDIS_PORT);
            final SchedulerInitializer scheduler = new SchedulerInitializer(redisCache);
            scheduler.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                scheduler.stop();
                redisCache.close();
            }));

            final TelegramBotsLongPollingApplication botsApplication =
                new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(
                PropertiesLoader.get("tgApiKey"),
                new SiftBot(redisCache)
            );
        } catch (TelegramApiException tgApiEx) {
            tgApiEx.printStackTrace();
        }
    }
}
