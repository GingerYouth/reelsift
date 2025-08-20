package main.java.sift.cache;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import main.java.sift.Session;
import redis.clients.jedis.Jedis;

/**
 * Provides caching functionality for `Session` objects using Redis.
 * Stores today's sessions and retrieves them as needed.
 */
public class RedisCache {
    private final Jedis jedis;
    public static final String KEY_PREFIX = "sessions:";

    public RedisCache(String host, int port) {
        this.jedis = new Jedis(host, port);
    }

    public void cacheSessions(final List<Session> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        final Map<LocalDate, List<Session>> sessionsByDate = sessions.stream()
            .collect(Collectors.groupingBy(s -> s.dateTime().toLocalDate()));
        for (final Map.Entry<LocalDate, List<Session>> entry : sessionsByDate.entrySet()) {
            final String key = KEY_PREFIX + entry.getKey().toString();
            this.jedis.set(key, Session.toJson(entry.getValue()));
            this.jedis.expire(
                key,
                LocalDateTime.now().until(
                    entry.getKey().atTime(LocalTime.MAX),
                    ChronoUnit.SECONDS
                )
            );
        }
    }

    public List<Session> getCachedSessions(final LocalDate date) {
        if (date == null) {
            return null;
        }
        final String cached = this.jedis.get(KEY_PREFIX + date);
        if (cached == null) {
            return null;
        }
        return Session.fromJsonArray(cached);
    }
}