package scheduler;

import bots.services.SessionCacheManager;
import filters.DateInterval;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import parser.City;

/**
 * Job that caches movie sessions for all cities for the next 3 weeks.
 * Designed to run as a scheduled background job via JobRunr.
 */
public final class CacheJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheJob.class);

    private final SessionCacheManager cacheManager;
    private final ThreeWeekInterval intervalFactory;

    /**
     * Creates a cache job with the given cache manager.
     *
     * @param cacheManager The session cache manager to use for caching
     */
    public CacheJob(final SessionCacheManager cacheManager) {
        this(cacheManager, new ThreeWeekInterval());
    }

    /**
     * Creates a cache job with the given cache manager and interval factory.
     *
     * @param cacheManager The session cache manager to use for caching
     * @param intervalFactory The factory for creating the date interval
     */
    public CacheJob(
        final SessionCacheManager cacheManager,
        final ThreeWeekInterval intervalFactory
    ) {
        this.cacheManager = cacheManager;
        this.intervalFactory = intervalFactory;
    }

    /**
     * Caches sessions for all cities for the next 3 weeks.
     * This method is called by JobRunr at the scheduled time.
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void cacheAllCities() {
        LOGGER.info("Starting scheduled cache job for all cities");
        final DateInterval interval = this.intervalFactory.create();
        LOGGER.info("Caching sessions for interval: {}", interval);

        for (final City city : City.values()) {
            try {
                LOGGER.info("Caching sessions for city: {}", city);
                this.cacheManager.ensureCached(interval, city);
                LOGGER.info("Successfully cached sessions for city: {}", city);
            } catch (final IOException e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Failed to cache sessions for city {}: {}", city, e.getMessage());
                }
            } catch (final Exception e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Unexpected error caching sessions for city {}: {}", city, e.getMessage());
                }
            }
        }
        LOGGER.info("Completed scheduled cache job for all cities");
    }
}
