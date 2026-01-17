package cache;

import parser.City;
import parser.Session;
import redis.clients.jedis.Jedis;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provides caching functionality for `Session` objects using Redis.
 * Stores today's sessions and retrieves them as needed.
 */
public class RedisCache {
    private final Jedis jedis;

    public RedisCache(final String host, final int port) {
        this.jedis = new Jedis(host, port);
    }

    public void cacheSessions(final List<Session> sessions, final City city) {
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        final Map<LocalDate, List<Session>> sessionsByDate = sessions.stream()
            .collect(Collectors.groupingBy(s -> s.dateTime().toLocalDate()));
        for (final Map.Entry<LocalDate, List<Session>> entry : sessionsByDate.entrySet()) {
            final String key = city.asPrefix() + entry.getKey().toString();
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

    public List<Session> getCachedSessions(final LocalDate date, final City city) {
        if (date == null) {
            return Collections.emptyList();
        }
        final String cached = this.jedis.get(city.asPrefix() + date);
        if (cached == null) {
            return Collections.emptyList();
        }
        return Session.fromJsonArray(cached);
    }

    public List<LocalDate> getCachedDates(final City city) {
        return this.jedis.keys(city.asPrefix() + "*").stream()
            .map(key -> LocalDate.parse(key.substring(city.asPrefix().length())))
            .collect(Collectors.toList());
    }
}