package scheduler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import cache.RedisCache;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import parser.City;
import parser.Session;

/**
 * Unit tests for {@link SchedulerInitializer}.
 */
final class SchedulerInitializerTest {

    @Test
    void startInitializesWithoutError() {
        final FakeRedisCache fakeCache = new FakeRedisCache();
        final SchedulerInitializer scheduler = new SchedulerInitializer(fakeCache);
        boolean started = false;

        try {
            scheduler.start();
            started = true;
        } finally {
            scheduler.stop();
        }

        assertThat(
            "scheduler cant start without error",
            started,
            is(true)
        );
    }

    @Test
    void stopCompletesWithoutError() {
        final FakeRedisCache fakeCache = new FakeRedisCache();
        final SchedulerInitializer scheduler = new SchedulerInitializer(fakeCache);
        scheduler.start();
        boolean stopped = false;

        scheduler.stop();
        stopped = true;

        assertThat(
            "scheduler cant stop without error",
            stopped,
            is(true)
        );
    }

    @Test
    void stopIsIdempotent() {
        final FakeRedisCache fakeCache = new FakeRedisCache();
        final SchedulerInitializer scheduler = new SchedulerInitializer(fakeCache);
        scheduler.start();
        boolean stoppedTwice = false;

        scheduler.stop();
        scheduler.stop();
        stoppedTwice = true;

        assertThat(
            "scheduler cant be stopped twice",
            stoppedTwice,
            is(true)
        );
    }

    @Test
    void stopWithoutStartDoesNotThrow() {
        final FakeRedisCache fakeCache = new FakeRedisCache();
        final SchedulerInitializer scheduler = new SchedulerInitializer(fakeCache);
        boolean stoppedWithoutStart = false;

        scheduler.stop();
        stoppedWithoutStart = true;

        assertThat(
            "scheduler throws when stopped without starting",
            stoppedWithoutStart,
            is(true)
        );
    }

    /**
     * Fake implementation of RedisCache for testing.
     */
    private static final class FakeRedisCache extends RedisCache {

        FakeRedisCache() {
            super("localhost", 0);
        }

        @Override
        public List<LocalDate> getCachedDates(final City city) {
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
}
