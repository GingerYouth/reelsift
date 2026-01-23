package cache;

import parser.City;
import parser.Session;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Provides caching functionality for {@link Session} objects using Redis.
 * Stores cinema sessions by city and date to avoid re-fetching identical data
 * across multiple user searches.
 *
 * <h2>Key Structure</h2>
 * Keys are organized by city and date: {@code CITY:DATE} (e.g., {@code MOSCOW:2026-01-23})
 *
 * <h2>Expiration</h2>
 * Keys automatically expire at the end of their respective date (23:59:59),
 * ensuring sessions don't persist after they're no longer relevant.
 *
 * <h2>Thread Safety</h2>
 * Uses {@link JedisPool} for safe concurrent access from multiple bot users.
 */
public class RedisCache {
    private static final Logger LOGGER = Logger.getLogger(RedisCache.class.getName());
    private static final int DEFAULT_POOL_SIZE = 10;
    private static final long MINIMUM_TTL_SECONDS = 300; // 5 minutes safety margin
    private static final String KEY_SEPARATOR = ":";

    private final JedisPool jedisPool;

    /**
     * Creates a Redis cache with default connection pool settings.
     *
     * @param host Redis server host
     * @param port Redis server port
     */
    public RedisCache(final String host, final int port) {
        this(host, port, DEFAULT_POOL_SIZE);
    }

    /**
     * Creates a Redis cache with custom connection pool size.
     *
     * @param host     Redis server host
     * @param port     Redis server port
     * @param poolSize Maximum number of connections in the pool
     */
    public RedisCache(final String host, final int port, final int poolSize) {
        final JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(poolSize);
        config.setMaxIdle(poolSize / 2);
        config.setMinIdle(5);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        config.setBlockWhenExhausted(true);
        this.jedisPool = new JedisPool(config, host, port);
    }

    /**
     * Caches sessions grouped by date.
     * Sessions are stored as JSON and expire at the end of their respective date.
     *
     * @param sessions Sessions to cache
     * @param city     City for which sessions are cached
     */
    public void cacheSessions(final List<Session> sessions, final City city) {
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        try (final Jedis jedis = this.jedisPool.getResource()) {
            final Map<LocalDate, List<Session>> sessionsByDate = sessions.stream()
                .collect(Collectors.groupingBy(s -> s.dateTime().toLocalDate()));

            for (final Map.Entry<LocalDate, List<Session>> entry : sessionsByDate.entrySet()) {
                final String key = buildKey(city, entry.getKey());
                final String json = Session.toJson(entry.getValue());
                final long ttl = calculateTTL(entry.getKey());

                jedis.set(key, json);
                jedis.expire(key, ttl);

                LOGGER.fine(() -> String.format(
                    "Cached %d sessions for %s on %s (TTL: %d seconds)",
                    entry.getValue().size(), city.name(), entry.getKey(), ttl
                ));
            }
        } catch (final Exception e) {
            LOGGER.severe(() -> String.format(
                "Failed to cache sessions for city %s: %s", city.name(), e.getMessage()
            ));
        }
    }

    /**
     * Retrieves cached sessions for multiple dates.
     *
     * @param dates Dates to retrieve sessions for
     * @param city  City to retrieve sessions from
     * @return List of cached sessions, or empty list if none found
     */
    public List<Session> getCachedSessions(final List<LocalDate> dates, final City city) {
        if (dates == null || dates.isEmpty()) {
            return Collections.emptyList();
        }

        return dates.stream()
            .map(d -> getCachedSessions(d, city))
            .filter(sessions -> sessions != null && !sessions.isEmpty())
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves cached sessions for a single date.
     *
     * @param date Date to retrieve sessions for
     * @param city City to retrieve sessions from
     * @return List of cached sessions for the date, or empty list if none found
     */
    public List<Session> getCachedSessions(final LocalDate date, final City city) {
        if (date == null) {
            return Collections.emptyList();
        }

        try (final Jedis jedis = this.jedisPool.getResource()) {
            final String key = buildKey(city, date);
            final String cached = jedis.get(key);

            if (cached == null) {
                LOGGER.fine(() -> String.format(
                    "Cache miss for %s on %s", city.name(), date
                ));
                return Collections.emptyList();
            }

            final List<Session> sessions = Session.fromJsonArray(cached);
            LOGGER.fine(() -> String.format(
                "Cache hit: Retrieved %d sessions for %s on %s", sessions.size(), city.name(), date
            ));

            return sessions;
        } catch (final Exception e) {
            LOGGER.severe(() -> String.format(
                "Failed to retrieve cached sessions for %s on %s: %s",
                city.name(), date, e.getMessage()
            ));
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves all cached dates for a city.
     *
     * @param city City to retrieve cached dates for
     * @return List of dates with cached sessions, sorted in descending order
     */
    public List<LocalDate> getCachedDates(final City city) {
        try (final Jedis jedis = this.jedisPool.getResource()) {
            final String prefix = city.asPrefix();
            return jedis.keys(prefix + "*").stream()
                .map(key -> LocalDate.parse(key.substring(prefix.length())))
                .sorted()
                .collect(Collectors.toList());
        } catch (final Exception e) {
            LOGGER.severe(() -> String.format(
                "Failed to retrieve cached dates for %s: %s", city.name(), e.getMessage()
            ));
            return Collections.emptyList();
        }
    }

    /**
     * Gets the number of cached date entries for a city.
     *
     * @param city City to check
     * @return Number of cached date entries
     */
    public long getCacheSize(final City city) {
        try (final Jedis jedis = this.jedisPool.getResource()) {
            return jedis.keys(city.asPrefix() + "*").size();
        } catch (final Exception e) {
            LOGGER.severe(() -> String.format(
                "Failed to get cache size for %s: %s", city.name(), e.getMessage()
            ));
            return 0;
        }
    }

    /**
     * Manually invalidates all cached sessions for a city.
     *
     * @param city City to invalidate
     */
    public void invalidateCity(final City city) {
        try (final Jedis jedis = this.jedisPool.getResource()) {
            final Set<String> keys = jedis.keys(city.asPrefix() + "*");
            if (!keys.isEmpty()) {
                jedis.del(keys.toArray(new String[0]));
                LOGGER.info(() -> String.format(
                    "Invalidated %d cache entries for %s", keys.size(), city.name()
                ));
            }
        } catch (final Exception e) {
            LOGGER.severe(() -> String.format(
                "Failed to invalidate cache for %s: %s", city.name(), e.getMessage()
            ));
        }
    }

    /**
     * Manually invalidates cached sessions for a specific date.
     *
     * @param date Date to invalidate
     * @param city City to invalidate
     */
    public void invalidateDate(final LocalDate date, final City city) {
        if (date == null) {
            return;
        }

        try (final Jedis jedis = this.jedisPool.getResource()) {
            final String key = buildKey(city, date);
            if (jedis.del(key) > 0) {
                LOGGER.info(() -> String.format(
                    "Invalidated cache for %s on %s", city.name(), date
                ));
            }
        } catch (final Exception e) {
            LOGGER.severe(() -> String.format(
                "Failed to invalidate cache for %s on %s: %s",
                city.name(), date, e.getMessage()
            ));
        }
    }

    /**
     * Updates sessions for a specific date (replaces existing cache).
     * Useful for partial refreshes without clearing all cached dates.
     *
     * @param sessions New sessions to store
     * @param date     Date to update
     * @param city     City to update
     */
    public void updateSessionsForDate(final List<Session> sessions, final LocalDate date, final City city) {
        if (date == null || sessions == null || sessions.isEmpty()) {
            return;
        }

        try (final Jedis jedis = this.jedisPool.getResource()) {
            final String key = buildKey(city, date);
            jedis.del(key);
            jedis.set(key, Session.toJson(sessions));
            jedis.expire(key, calculateTTL(date));

            LOGGER.info(() -> String.format(
                "Updated %d sessions for %s on %s", sessions.size(), city.name(), date
            ));
        } catch (final Exception e) {
            LOGGER.severe(() -> String.format(
                "Failed to update sessions for %s on %s: %s",
                city.name(), date, e.getMessage()
            ));
        }
    }

    /**
     * Closes the connection pool and releases resources.
     * Should be called on application shutdown.
     */
    public void close() {
        if (this.jedisPool != null && !this.jedisPool.isClosed()) {
            this.jedisPool.close();
            LOGGER.info("Redis connection pool closed");
        }
    }

    /**
     * Builds a cache key for the given city and date.
     *
     * @param city City component
     * @param date Date component
     * @return Cache key in format {@code CITY:DATE}
     */
    private String buildKey(final City city, final LocalDate date) {
        return city.asPrefix() + date;
    }

    /**
     * Calculates TTL (time-to-live) for a cache entry.
     * Sessions expire at the end of their respective date.
     *
     * @param date Date of the sessions
     * @return TTL in seconds, with a minimum safety margin
     */
    private long calculateTTL(final LocalDate date) {
        final long ttl = LocalDateTime.now().until(
            date.atTime(LocalTime.MAX),
            ChronoUnit.SECONDS
        );
        return Math.max(ttl, MINIMUM_TTL_SECONDS);
    }
}
