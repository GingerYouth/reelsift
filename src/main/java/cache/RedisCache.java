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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Provides caching functionality for {@link Session} objects using Redis.
 * Stores cinema sessions by city, date, and film to avoid re-fetching identical data
 * across multiple user searches.
 *
 * <h2>Key Structure</h2>
 * Keys are organized by city, date, and film: {@code CITY:DATE:FILM} (e.g., {@code MOSCOW:2026-01-23:FilmName})
 *
 * <h2>Expiration</h2>
 * Keys automatically expire at the end of their respective date (23:59:59),
 * ensuring sessions don't persist after they're no longer relevant.
 *
 * <h2>Thread Safety</h2>
 * Uses {@link JedisPool} for safe concurrent access from multiple bot users.
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class RedisCache {
    private static final Logger LOGGER = Logger.getLogger(RedisCache.class.getName());
    private static final int DEFAULT_POOL_SIZE = 10;
    private static final long MINIMUM_TTL_SECONDS = 300; // 5 minutes safety margin

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
     * Caches sessions grouped by date and film.
     * Sessions are stored as JSON and expire at the end of their respective date.
     *
     * @param sessions Sessions to cache
     * @param city     City for which sessions are cached
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public void cacheSessions(final List<Session> sessions, final City city) {
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        try (Jedis jedis = this.jedisPool.getResource()) {
            final Map<LocalDate, Map<String, List<Session>>> sessionsByDateAndFilm = sessions.stream()
                .collect(Collectors.groupingBy(
                    s -> s.dateTime().toLocalDate(),
                    Collectors.groupingBy(Session::name)
                ));

            for (final Map.Entry<LocalDate, Map<String, List<Session>>> dateEntry : sessionsByDateAndFilm.entrySet()) {
                final LocalDate date = dateEntry.getKey();
                final long ttl = calculateTTL(date);

                for (final Map.Entry<String, List<Session>> filmEntry : dateEntry.getValue().entrySet()) {
                    final String filmName = filmEntry.getKey();
                    final List<Session> filmSessions = filmEntry.getValue();
                    final String key = this.buildKey(city, date, filmName);

                    // Get existing sessions and merge with new ones
                    final String existingJson = jedis.get(key);
                    final List<Session> allSessions = new ArrayList<>();
                    if (existingJson != null) {
                        allSessions.addAll(Session.fromJsonArray(existingJson));
                    }
                    allSessions.addAll(filmSessions);

                    // Remove duplicates by using a set of unique session identifiers
                    final List<Session> uniqueSessions = allSessions.stream()
                        .distinct()
                        .collect(Collectors.toList());

                    final String json = Session.toJson(uniqueSessions);
                    jedis.set(key, json);
                    jedis.expire(key, ttl);

                    LOGGER.fine(() -> String.format(
                        "Cached %d sessions (%d new, %d existing) for %s on %s - %s (TTL: %d seconds)",
                        uniqueSessions.size(), filmSessions.size(), 
                        allSessions.size() - filmSessions.size(), city.name(), date, filmName, ttl
                    ));
                }
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

        try (Jedis jedis = this.jedisPool.getResource()) {
            final String prefix = city.asPrefix() + date + ":";
            final Set<String> keys = jedis.keys(prefix + "*");
            
            if (keys.isEmpty()) {
                LOGGER.fine(() -> String.format(
                    "Cache miss for %s on %s", city.name(), date
                ));
                return Collections.emptyList();
            }

            final List<Session> allSessions = new ArrayList<>();
            for (final String key : keys) {
                final String cached = jedis.get(key);
                if (cached != null) {
                    final List<Session> sessions = Session.fromJsonArray(cached);
                    allSessions.addAll(sessions);
                }
            }

            LOGGER.fine(() -> String.format(
                "Cache hit: Retrieved %d sessions for %s on %s from %d film entries", 
                allSessions.size(), city.name(), date, keys.size()
            ));

            return allSessions;
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
        try (Jedis jedis = this.jedisPool.getResource()) {
            final String prefix = city.asPrefix();
            return jedis.keys(prefix + "*").stream()
                .map(key -> {
                    final String datePart = key.substring(prefix.length());
                    // Extract date part before the first colon (if it exists)
                    final int colonIndex = datePart.indexOf(':');
                    return colonIndex > 0 ? datePart.substring(0, colonIndex) : datePart;
                })
                .distinct() // Remove duplicates since we now have multiple films per date
                .map(LocalDate::parse)
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
        try (Jedis jedis = this.jedisPool.getResource()) {
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
        try (Jedis jedis = this.jedisPool.getResource()) {
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

        try (Jedis jedis = this.jedisPool.getResource()) {
            final String prefix = city.asPrefix() + date + ":";
            final Set<String> keys = jedis.keys(prefix + "*");
            if (!keys.isEmpty()) {
                jedis.del(keys.toArray(new String[0]));
                LOGGER.info(() -> String.format(
                    "Invalidated %d cache entries for %s on %s", keys.size(), city.name(), date
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
     * Manually invalidates all cached entries.
     * Clears the entire Redis cache of all sessions.
     */
    public void invalidateAll() {
        try (Jedis jedis = this.jedisPool.getResource()) {
            final Set<String> keys = jedis.keys("*");
            if (!keys.isEmpty()) {
                jedis.del(keys.toArray(new String[0]));
                LOGGER.info(() -> String.format(
                    "Invalidated all %d cache entries", keys.size()
                ));
            }
        } catch (final Exception e) {
            LOGGER.severe(() -> String.format(
                "Failed to invalidate all cache entries: %s", e.getMessage()
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
     * Builds a cache key for the given city, date, and film.
     *
     * @param city     City component
     * @param date     Date component
     * @param filmName Film name component
     * @return Cache key in format {@code CITY:DATE:FILM}
     */
    private String buildKey(final City city, final LocalDate date, final String filmName) {
        return city.asPrefix() + date + ":" + filmName;
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
