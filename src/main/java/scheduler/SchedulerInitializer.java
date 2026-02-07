package scheduler;

import bots.services.SessionCacheManager;
import cache.RedisCache;
import java.time.ZoneId;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.scheduling.cron.Cron;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes and manages the JobRunr scheduler for background session caching.
 * Uses InMemoryStorageProvider since the application runs as a single instance.
 */
public final class SchedulerInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerInitializer.class);
    private static final String JOB_ID = "daily-session-cache";
    private static final int SCHEDULE_HOUR = 2;
    private static final int SCHEDULE_MINUTE = 55;
    private static final String MOSCOW_TIMEZONE = "Europe/Moscow";

    private final RedisCache redisCache;
    private BackgroundJobServer backgroundJobServer;
    private InMemoryStorageProvider storageProvider;

    /**
     * Creates a scheduler initializer with the given Redis cache.
     *
     * @param redisCache The Redis cache to use for session caching
     */
    public SchedulerInitializer(final RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    /**
     * Starts the scheduler and schedules the daily cache job.
     * The job runs at specified Moscow time.
     */
    public void start() {
        LOGGER.info("Starting scheduler");

        final CacheJobActivator activator = new CacheJobActivator();
        final SessionCacheManager cacheManager = new SessionCacheManager(this.redisCache);
        final CacheJob cacheJob = new CacheJob(cacheManager);
        activator.register(CacheJob.class, cacheJob);

        this.storageProvider = new InMemoryStorageProvider();

        JobRunr.configure()
            .useStorageProvider(this.storageProvider)
            .useJobActivator(activator)
            .useBackgroundJobServer()
            .initialize();

        this.backgroundJobServer = JobRunr.getBackgroundJobServer();

        BackgroundJob.<CacheJob>scheduleRecurrently(
            JOB_ID,
            Cron.daily(SCHEDULE_HOUR, SCHEDULE_MINUTE),
            ZoneId.of(MOSCOW_TIMEZONE),
            job -> job.cacheAllCities()
        );

        LOGGER.info(
            "Scheduled daily cache job '{}' at {}:{} {}",
            JOB_ID, SCHEDULE_HOUR, SCHEDULE_MINUTE, MOSCOW_TIMEZONE
        );
    }

    /**
     * Stops the scheduler gracefully.
     */
    public void stop() {
        LOGGER.info("Stopping scheduler");
        if (this.backgroundJobServer != null) {
            this.backgroundJobServer.stop();
            LOGGER.info("Scheduler stopped");
        }
        if (this.storageProvider != null) {
            this.storageProvider.close();
        }
    }
}
