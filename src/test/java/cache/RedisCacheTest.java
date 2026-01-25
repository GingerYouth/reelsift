package cache;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import parser.City;
import parser.Session;
import redis.clients.jedis.Jedis;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link RedisCache}.
 * Uses integration testing approach with real Redis connection and fails when Redis is not accessible.
 */
public class RedisCacheTest {

    private RedisCache redisCache;
    private static final String TEST_HOST = "localhost";
    private static final int TEST_PORT = 6379;
    private static final int TEST_POOL_SIZE = 5;
    
    private boolean redisAvailable = false;
    private static final String REDIS_UNAVAILABLE_MESSAGE = 
        "Redis is not available, but test requires Redis connection";

    @Before
    public void setUp() {
        try (Jedis testJedis = new Jedis(TEST_HOST, TEST_PORT, 1000, 1000)) {
            testJedis.ping();
            this.redisAvailable = true;
        } catch (final Exception e) {
            this.redisAvailable = false;
        }

        if (this.redisAvailable) {
            this.redisCache = new RedisCache(TEST_HOST, TEST_PORT, TEST_POOL_SIZE);
            // Clean up any existing test data
            try (Jedis jedis = new Jedis(TEST_HOST, TEST_PORT)) {
                Set<String> keys = jedis.keys("MOSCOW:*");
                if (!keys.isEmpty()) {
                    jedis.del(keys.toArray(new String[0]));
                }
                keys = jedis.keys("SPB:*");
                if (!keys.isEmpty()) {
                    jedis.del(keys.toArray(new String[0]));
                }
            } catch (final Exception ignored) {
                // Ignore cleanup errors
            }
        }
    }

    @After
    public void tearDown() {
        if (this.redisCache != null && this.redisAvailable) {
            // Clean up test data
            try (Jedis jedis = new Jedis(TEST_HOST, TEST_PORT)) {
                Set<String> keys = jedis.keys("MOSCOW:*");
                if (!keys.isEmpty()) {
                    jedis.del(keys.toArray(new String[0]));
                }
                keys = jedis.keys("SPB:*");
                if (!keys.isEmpty()) {
                    jedis.del(keys.toArray(new String[0]));
                }
            } catch (Exception e) {
                // Ignore cleanup errors
            }
            this.redisCache.close();
        }
    }

    @Test
    public void cacheConstruction() {
        if (!this.redisAvailable) {
            fail(REDIS_UNAVAILABLE_MESSAGE);
        }
        final RedisCache cache = new RedisCache(TEST_HOST, TEST_PORT);
        assertNotNull(cache);
        cache.close();
    }

    @Test
    public void constructionWithCustomPoolSize() {
        if (!this.redisAvailable) {
            fail(REDIS_UNAVAILABLE_MESSAGE);
        }
        final RedisCache cache = new RedisCache(TEST_HOST, TEST_PORT, TEST_POOL_SIZE);
        assertNotNull(cache);
        cache.close();
    }

    @Test
    public void cacheSessionsWithValidData() {
        if (!this.redisAvailable) {
            fail(REDIS_UNAVAILABLE_MESSAGE);
        }
        final List<Session> sessions = createSession();
        final LocalDate testDate = LocalDate.of(2024, 1, 1);
        this.redisCache.cacheSessions(sessions, City.MOSCOW);
        
        final List<Session> cachedSessions = this.redisCache.getCachedSessions(testDate, City.MOSCOW);
        assertEquals(1, cachedSessions.size());
        assertEquals("Test Movie", cachedSessions.get(0).name());
    }

    @Test
    public void cacheSessionsWithEmptyList() {
        if (!this.redisAvailable) {
            fail(REDIS_UNAVAILABLE_MESSAGE);
        }
        this.redisCache.cacheSessions(Collections.emptyList(), City.MOSCOW);

        long cacheSize = this.redisCache.getCacheSize(City.MOSCOW);
        assertEquals(0, cacheSize);
    }

    @Test
    public void cacheSessionsWithNullList() {
        if (!this.redisAvailable) {
            fail(REDIS_UNAVAILABLE_MESSAGE);
        }
        this.redisCache.cacheSessions(null, City.MOSCOW);

        long cacheSize = this.redisCache.getCacheSize(City.MOSCOW);
        assertEquals(0, cacheSize);
    }

    @Test
    public void cacheSessionsWithException() {
        // Test with invalid Redis connection to simulate exception
        final RedisCache cacheWithInvalidConnection = new RedisCache("invalid-host", 9999);
        final List<Session> sessions = createSession();
        try {
            cacheWithInvalidConnection.cacheSessions(sessions, City.MOSCOW);
        } catch (final Exception e) {
            fail("Should not throw exception, just log it: " + e.getMessage());
        } finally {
            cacheWithInvalidConnection.close();
        }
    }

    @Test
    public void getCachedSessionsSingleDateWithHit() {
        if (!this.redisAvailable) {
            fail(REDIS_UNAVAILABLE_MESSAGE);
        }
        final LocalDate testDate = LocalDate.of(2024, 1, 1);
        this.redisCache.cacheSessions(createSession(), City.MOSCOW);

        final List<Session> result = this.redisCache.getCachedSessions(testDate, City.MOSCOW);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Movie", result.get(0).name());
    }

    @Test
    public void getCachedSessionsSingleDateWithMiss() {
        if (!this.redisAvailable) {
            fail(REDIS_UNAVAILABLE_MESSAGE);
        }
        final LocalDate testDate = LocalDate.of(2024, 1, 1);

        final List<Session> result = this.redisCache.getCachedSessions(testDate, City.MOSCOW);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getCachedSessionsSingleDateWithNullDate() {
        if (!this.redisAvailable) {
            fail(REDIS_UNAVAILABLE_MESSAGE);
        }

        final List<Session> result = this.redisCache.getCachedSessions((LocalDate) null, City.MOSCOW);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getCachedSessionsSingleDateWithException() {
        // Test with invalid Redis connection to simulate exception
        final RedisCache cacheWithInvalidConnection = new RedisCache("invalid-host", 9999);
        final LocalDate testDate = LocalDate.of(2024, 1, 1);

        final List<Session> result = cacheWithInvalidConnection.getCachedSessions(testDate, City.MOSCOW);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        cacheWithInvalidConnection.close();
    }

    @Test
    public void getCachedSessionsMultipleDates() {
        if (!this.redisAvailable) {
            fail(REDIS_UNAVAILABLE_MESSAGE);
        }
        final List<Session> sessions = Arrays.asList(
            new Session(
                LocalDateTime.of(2024, 1, 1, 10, 30),
                "Movie1", "Desc1", "Verdict1",
                Arrays.asList("Action"), "Cinema1", "Address1",
                500, "link1", true
            ),
            new Session(
                LocalDateTime.of(2024, 1, 2, 10, 30),
                "Movie2", "Desc2", "Verdict2",
                Arrays.asList("Drama"), "Cinema2", "Address2",
                600, "link2", false
            )
        );
        this.redisCache.cacheSessions(sessions, City.MOSCOW);

        final LocalDate date1 = LocalDate.of(2024, 1, 1);
        final LocalDate date2 = LocalDate.of(2024, 1, 2);
        final List<LocalDate> dates = Arrays.asList(date1, date2);
        final List<Session> result = this.redisCache.getCachedSessions(dates, City.MOSCOW);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(s -> s.name().equals("Movie1")));
        assertTrue(result.stream().anyMatch(s -> s.name().equals("Movie2")));
    }

    @Test
    public void getCachedSessionsEmptyList() {
        if (!this.redisAvailable) {
            fail(REDIS_UNAVAILABLE_MESSAGE);
        }

        final List<Session> result = this.redisCache.getCachedSessions(
            Collections.emptyList(), City.MOSCOW
        );
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getCacheSize() {
        if (!this.redisAvailable) {
            fail(REDIS_UNAVAILABLE_MESSAGE);
        }
        final List<Session> sessions = createSession();
        this.redisCache.cacheSessions(sessions, City.MOSCOW);

        long result = redisCache.getCacheSize(City.MOSCOW);
        assertEquals(1, result);
    }

    @Test
    public void getCacheSizeWithException() {
        // Test with invalid Redis connection to simulate exception
        final RedisCache cacheWithInvalidConnection = new RedisCache("invalid-host", 9999);

        long result = cacheWithInvalidConnection.getCacheSize(City.MOSCOW);
        assertEquals(0, result);
        cacheWithInvalidConnection.close();
    }

    @Test
    public void invalidateCity() {
        if (!this.redisAvailable) {
            fail(REDIS_UNAVAILABLE_MESSAGE);
        }

        // Cache some data first
        this.redisCache.cacheSessions(createSession(), City.MOSCOW);
        assertEquals(1, this.redisCache.getCacheSize(City.MOSCOW));

        // Then invalidate
        this.redisCache.invalidateCity(City.MOSCOW);

        // Verify data was invalidated
        assertEquals(0, this.redisCache.getCacheSize(City.MOSCOW));
    }

    @Test
    public void invalidateCityWithNoKeys() {
        if (!this.redisAvailable) {
            fail(REDIS_UNAVAILABLE_MESSAGE);
        }

        // Should not throw exception even when no keys exist
        this.redisCache.invalidateCity(City.SPB);

        // Verify cache size is still 0
        assertEquals(0, this.redisCache.getCacheSize(City.SPB));
    }

    @Test
    public void invalidateCityWithException() {
        // Test with invalid Redis connection to simulate exception
        final RedisCache cacheWithInvalidConnection = new RedisCache("invalid-host", 9999);

        try {
            cacheWithInvalidConnection.invalidateCity(City.MOSCOW);
        } catch (final Exception e) {
            fail("Should not throw exception, just log it: " + e.getMessage());
        } finally {
            cacheWithInvalidConnection.close();
        }
    }

    @Test
    public void invalidateDate() {
        if (!this.redisAvailable) {
            fail(REDIS_UNAVAILABLE_MESSAGE);
        }
        final LocalDate testDate = LocalDate.of(2024, 1, 1);
        // Cache some data first
        this.redisCache.cacheSessions(createSession(), City.MOSCOW);
        assertEquals(1, this.redisCache.getCacheSize(City.MOSCOW));

        // Then invalidate specific date
        this.redisCache.invalidateDate(testDate, City.MOSCOW);

        // Verify data was invalidated
        assertEquals(0, this.redisCache.getCacheSize(City.MOSCOW));
    }

    @Test
    public void invalidateDateWithNullDate() {
        if (!this.redisAvailable) {
            fail(REDIS_UNAVAILABLE_MESSAGE);
        }

        // Should not throw exception
        this.redisCache.invalidateDate(null, City.MOSCOW);
    }

    @Test
    public void invalidateDateWithException() {
        // Test with invalid Redis connection to simulate exception
        final RedisCache cacheWithInvalidConnection = new RedisCache("invalid-host", 9999);

        try {
            cacheWithInvalidConnection.invalidateDate(
                LocalDate.of(2024, 1, 1),
                City.MOSCOW
            );
        } catch (Exception e) {
            fail("Should not throw exception, just log it: " + e.getMessage());
        } finally {
            cacheWithInvalidConnection.close();
        }
    }

    @Test
    public void testClose() {
        if (!this.redisAvailable) {
            fail(REDIS_UNAVAILABLE_MESSAGE);
        }
        try {
            this.redisCache.close();
        } catch (final Exception e) {
            fail("Close should not throw exception: " + e.getMessage());
        }
    }

    @Test
    public void calculateTTL() throws Exception {
        if (!this.redisAvailable) {
            fail(REDIS_UNAVAILABLE_MESSAGE);
        }
        final Method calculateTTLMethod = RedisCache.class.getDeclaredMethod("calculateTTL", LocalDate.class);
        calculateTTLMethod.setAccessible(true);

        final LocalDate today = LocalDate.now();
        final LocalDate tomorrow = today.plusDays(1);

        long resultToday = (Long) calculateTTLMethod.invoke(this.redisCache, today);
        long resultTomorrow = (Long) calculateTTLMethod.invoke(this.redisCache, tomorrow);

        // TTL should be at least the minimum (300 seconds)
        assertTrue(resultToday >= 300);
        assertTrue(resultTomorrow >= 300);

        // Tomorrow should have longer TTL than today
        assertTrue(resultTomorrow > resultToday);
    }

    @Test
    public void calculateTTLWithPastDate() throws Exception {
        if (!this.redisAvailable) {
            fail(REDIS_UNAVAILABLE_MESSAGE);
        }
        final Method calculateTTLMethod = RedisCache.class.getDeclaredMethod("calculateTTL", LocalDate.class);
        calculateTTLMethod.setAccessible(true);

        final LocalDate pastDate = LocalDate.now().minusDays(1);
        long result = (Long) calculateTTLMethod.invoke(this.redisCache, pastDate);
        // Should return minimum TTL for past dates
        assertEquals(300, result);
    }

    private static List<Session> createSession() {
        return List.of(
            new Session(
                LocalDateTime.of(2024, 1, 1, 10, 30),
                "Test Movie",
                "Description",
                "Verdict",
                Arrays.asList("Action", "Drama"),
                "Test Cinema",
                "Test Address",
                500,
                "test-link",
                true
            )
        );
    }
}