package bots.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import parser.City;
import parser.Session;

/**
 * Unit tests for {@link SessionCacheManager}.
 * Uses fake implementations to avoid Redis and network dependencies.
 */
final class SessionCacheManagerTest {

    @Test
    void getCachedSessionsReturnsSessionsFromCache() {
        final FakeRedisCache fakeCache = new FakeRedisCache();
        final City city = randomCity();
        final LocalDate date = randomFutureDate();
        final Session session = randomSession(date);
        fakeCache.cacheSessions(List.of(session), city);
        final SessionCacheManager manager = new SessionCacheManager(fakeCache);

        assertThat(
            "manager cant return sessions that were cached",
            manager.getCachedSessions(List.of(date), city),
            hasSize(1)
        );
    }

    @Test
    void getCachedSessionsReturnsEmptyListWhenNothingCached() {
        final FakeRedisCache fakeCache = new FakeRedisCache();
        final SessionCacheManager manager = new SessionCacheManager(fakeCache);

        assertThat(
            "manager cant return empty list when cache is empty",
            manager.getCachedSessions(List.of(randomFutureDate()), randomCity()),
            is(empty())
        );
    }

    @Test
    void getCachedSessionsReturnsEmptyListForEmptyDateList() {
        final FakeRedisCache fakeCache = new FakeRedisCache();
        final SessionCacheManager manager = new SessionCacheManager(fakeCache);

        assertThat(
            "manager cant return empty list for empty date list",
            manager.getCachedSessions(Collections.emptyList(), randomCity()),
            is(empty())
        );
    }

    @Test
    void getCachedSessionsReturnsSessionsForMultipleDates() {
        final FakeRedisCache fakeCache = new FakeRedisCache();
        final City city = randomCity();
        final LocalDate date1 = randomFutureDate();
        final LocalDate date2 = date1.plusDays(1);
        final Session session1 = randomSession(date1);
        final Session session2 = randomSession(date2);
        fakeCache.cacheSessions(List.of(session1), city);
        fakeCache.cacheSessions(List.of(session2), city);
        final SessionCacheManager manager = new SessionCacheManager(fakeCache);

        assertThat(
            "manager cant return sessions for multiple dates",
            manager.getCachedSessions(List.of(date1, date2), city),
            hasSize(2)
        );
    }

    @Test
    void getCachedSessionsReturnsOnlySessionsForRequestedDates() {
        final FakeRedisCache fakeCache = new FakeRedisCache();
        final City city = randomCity();
        final LocalDate date1 = randomFutureDate();
        final LocalDate date2 = date1.plusDays(1);
        final LocalDate date3 = date1.plusDays(2);
        fakeCache.cacheSessions(List.of(randomSession(date1)), city);
        fakeCache.cacheSessions(List.of(randomSession(date2)), city);
        fakeCache.cacheSessions(List.of(randomSession(date3)), city);
        final SessionCacheManager manager = new SessionCacheManager(fakeCache);

        assertThat(
            "manager cant return only sessions for requested dates",
            manager.getCachedSessions(List.of(date1, date3), city),
            hasSize(2)
        );
    }

    @Test
    void getCachedSessionsReturnsSessionsOnlyForRequestedCity() {
        final FakeRedisCache fakeCache = new FakeRedisCache();
        final LocalDate date = randomFutureDate();
        fakeCache.cacheSessions(List.of(randomSession(date)), City.MOSCOW);
        fakeCache.cacheSessions(List.of(randomSession(date)), City.SPB);
        final SessionCacheManager manager = new SessionCacheManager(fakeCache);

        assertThat(
            "manager cant return sessions only for requested city",
            manager.getCachedSessions(List.of(date), City.MOSCOW),
            hasSize(1)
        );
    }

    @Test
    void getCachedSessionsPreservesSessionData() {
        final FakeRedisCache fakeCache = new FakeRedisCache();
        final City city = randomCity();
        final LocalDate date = randomFutureDate();
        final String expectedName = UUID.randomUUID().toString();
        final Session session = new Session(
            date.atTime(randomHour(), randomMinute()),
            expectedName,
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            List.of(UUID.randomUUID().toString()),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            new Random().nextInt(1000),
            UUID.randomUUID().toString(),
            new Random().nextBoolean()
        );
        fakeCache.cacheSessions(List.of(session), city);
        final SessionCacheManager manager = new SessionCacheManager(fakeCache);

        assertThat(
            "manager cant preserve session name from cache",
            manager.getCachedSessions(List.of(date), city).get(0).name(),
            is(equalTo(expectedName))
        );
    }

    private static City randomCity() {
        final City[] cities = City.values();
        return cities[new Random().nextInt(cities.length)];
    }

    private static LocalDate randomFutureDate() {
        return LocalDate.now().plusDays(new Random().nextInt(30) + 1);
    }

    private static int randomHour() {
        return new Random().nextInt(24);
    }

    private static int randomMinute() {
        return new Random().nextInt(60);
    }

    private static Session randomSession(final LocalDate date) {
        return new Session(
            date.atTime(randomHour(), randomMinute()),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            List.of(UUID.randomUUID().toString()),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            new Random().nextInt(1000),
            UUID.randomUUID().toString(),
            new Random().nextBoolean()
        );
    }

    /**
     * Fake implementation of RedisCache that stores sessions in memory.
     * Extends RedisCache but does not require actual Redis connection.
     */
    private static final class FakeRedisCache extends cache.RedisCache {
        private final Map<City, Map<LocalDate, List<Session>>> storage = new HashMap<>();

        FakeRedisCache() {
            super("localhost", 0);
        }

        @Override
        public void cacheSessions(final List<Session> sessions, final City city) {
            if (sessions == null || sessions.isEmpty()) {
                return;
            }
            this.storage.computeIfAbsent(city, k -> new HashMap<>());
            for (final Session session : sessions) {
                final LocalDate date = session.dateTime().toLocalDate();
                this.storage.get(city)
                    .computeIfAbsent(date, k -> new ArrayList<>())
                    .add(session);
            }
        }

        @Override
        public List<Session> getCachedSessions(final List<LocalDate> dates, final City city) {
            if (dates == null || dates.isEmpty()) {
                return Collections.emptyList();
            }
            final Map<LocalDate, List<Session>> cityStorage = this.storage.get(city);
            if (cityStorage == null) {
                return Collections.emptyList();
            }
            return dates.stream()
                .filter(cityStorage::containsKey)
                .flatMap(date -> cityStorage.get(date).stream())
                .toList();
        }

        @Override
        public List<LocalDate> getCachedDates(final City city) {
            final Map<LocalDate, List<Session>> cityStorage = this.storage.get(city);
            if (cityStorage == null) {
                return Collections.emptyList();
            }
            return new ArrayList<>(cityStorage.keySet());
        }

        @Override
        public void close() {
            this.storage.clear();
        }
    }
}
