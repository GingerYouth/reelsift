package scheduler;

import bots.services.SessionCacheManager;
import cache.RedisCache;
import filters.DateInterval;
import org.junit.jupiter.api.Test;
import parser.AfishaParser;
import parser.City;
import parser.Session;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CacheJob}.
 */
final class CacheJobTest {

    @Test
    void cacheAllCitiesCachesAllThreeCities() {
        final SpyRedisCache spyCache = new SpyRedisCache();
        final TestSessionCacheManager cacheManager = new TestSessionCacheManager(spyCache);
        final LocalDate today = LocalDate.of(2025, 6, 15);
        final ThreeWeekInterval intervalFactory = new ThreeWeekInterval(today);
        final CacheJob job = new CacheJob(cacheManager, intervalFactory);

        job.cacheAllCities();

        assertThat(
            "job doesnt cache all three cities",
            spyCache.getCitiesCached(),
            containsInAnyOrder(City.MOSCOW, City.SPB, City.BALASHIHA)
        );
    }

    @Test
    void cacheAllCitiesUsesThreeWeekInterval() {
        final SpyRedisCache spyCache = new SpyRedisCache();
        final TestSessionCacheManager cacheManager = new TestSessionCacheManager(spyCache);
        final LocalDate today = LocalDate.of(2025, 6, 15);
        final ThreeWeekInterval intervalFactory = new ThreeWeekInterval(today);
        final CacheJob job = new CacheJob(cacheManager, intervalFactory);

        job.cacheAllCities();

        assertThat(
            "job doesnt use correct start date",
            spyCache.getIntervalsChecked().stream()
                .allMatch(i -> i.start().equals(today)),
            is(true)
        );
    }

    @Test
    void cacheAllCitiesContinuesOnParseFailure() {
        final FailingRedisCache failingCache = new FailingRedisCache(City.MOSCOW);
        final TestSessionCacheManager cacheManager = new TestSessionCacheManager(failingCache);
        final LocalDate today = LocalDate.of(2025, 6, 15);
        final ThreeWeekInterval intervalFactory = new ThreeWeekInterval(today);
        final CacheJob job = new CacheJob(cacheManager, intervalFactory);

        job.cacheAllCities();

        assertThat(
            "job doesnt continue after failure",
            failingCache.getCitiesAttempted().size() >= 2,
            is(true)
        );
    }

    /**
     * Spy implementation of RedisCache that records which cities were cached.
     */
    private static final class SpyRedisCache extends RedisCache {
        private final List<City> citiesCached;
        private final List<DateInterval> intervalsChecked;
        private final Map<City, Map<LocalDate, List<Session>>> storage;

        SpyRedisCache() {
            super("localhost", 0);
            this.citiesCached = new ArrayList<>();
            this.intervalsChecked = new ArrayList<>();
            this.storage = new HashMap<>();
        }

        List<City> getCitiesCached() {
            return new ArrayList<>(this.citiesCached);
        }

        List<DateInterval> getIntervalsChecked() {
            return new ArrayList<>(this.intervalsChecked);
        }

        @Override
        public List<LocalDate> getCachedDates(final City city) {
            if (!this.citiesCached.contains(city)) {
                this.citiesCached.add(city);
            }
            return Collections.emptyList();
        }

        @Override
        public void cacheSessions(final List<Session> sessions, final City city) {
            this.storage.computeIfAbsent(city, k -> new HashMap<>());
        }

        @Override
        public List<Session> getCachedSessions(final List<LocalDate> dates, final City city) {
            return Collections.emptyList();
        }

        @Override
        public void close() {
            this.storage.clear();
        }
    }

    /**
     * Redis cache that fails for a specific city to test error handling.
     */
    private static final class FailingRedisCache extends RedisCache {
        private final City failCity;
        private final List<City> citiesAttempted;

        FailingRedisCache(final City failCity) {
            super("localhost", 0);
            this.failCity = failCity;
            this.citiesAttempted = new ArrayList<>();
        }

        List<City> getCitiesAttempted() {
            return new ArrayList<>(this.citiesAttempted);
        }

        @Override
        public List<LocalDate> getCachedDates(final City city) {
            this.citiesAttempted.add(city);
            if (city == this.failCity) {
                throw new RuntimeException("Simulated failure for " + city);
            }
            return Collections.emptyList();
        }

        @Override
        public void cacheSessions(final List<Session> sessions, final City city) {
        }

        @Override
        public List<Session> getCachedSessions(final List<LocalDate> dates, final City city) {
            return Collections.emptyList();
        }

        @Override
        public void close() {
        }
    }

    /**
     * Test-specific SessionCacheManager that provides a mocked AfishaParser.
     */
    private static final class TestSessionCacheManager extends SessionCacheManager {
        TestSessionCacheManager(final RedisCache redisCache) {
            super(redisCache);
        }

        @Override
        protected AfishaParser createAfishaParser(final City city) {
            final AfishaParser mockParser = mock(AfishaParser.class);
            try {
                when(mockParser.parseFilmsInDates(org.mockito.ArgumentMatchers.anyString()))
                    .thenReturn(Collections.emptyList());
                when(mockParser.parseSchedule(
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.anyString()
                )).thenReturn(Collections.emptyList());
            } catch (final IOException e) {
                // Should not happen in a mock
            }
            return mockParser;
        }
    }
}
