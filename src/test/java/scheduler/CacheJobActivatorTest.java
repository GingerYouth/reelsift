package scheduler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CacheJobActivator}.
 */
final class CacheJobActivatorTest {

    @Test
    void activateJobReturnsRegisteredInstance() {
        final CacheJobActivator activator = new CacheJobActivator();
        final FakeJob expected = new FakeJob(UUID.randomUUID().toString());
        activator.register(FakeJob.class, expected);

        assertThat(
            "activator doesnt return registered instance",
            activator.activateJob(FakeJob.class),
            is(sameInstance(expected))
        );
    }

    @Test
    void activateJobThrowsForUnregisteredType() {
        final CacheJobActivator activator = new CacheJobActivator();

        IllegalStateException thrown = null;
        try {
            activator.activateJob(FakeJob.class);
        } catch (final IllegalStateException e) {
            thrown = e;
        }

        assertThat(
            "activator doesnt throw for unregistered type",
            thrown != null,
            is(true)
        );
    }

    @Test
    void activateJobReturnsCorrectInstanceWhenMultipleRegistered() {
        final CacheJobActivator activator = new CacheJobActivator();
        final FakeJob job1 = new FakeJob(UUID.randomUUID().toString());
        final AnotherFakeJob job2 = new AnotherFakeJob(UUID.randomUUID().toString());
        activator.register(FakeJob.class, job1);
        activator.register(AnotherFakeJob.class, job2);

        assertThat(
            "activator doesnt return correct instance for first type",
            activator.activateJob(FakeJob.class),
            is(sameInstance(job1))
        );
    }

    @Test
    void activateJobReturnsSecondInstanceWhenMultipleRegistered() {
        final CacheJobActivator activator = new CacheJobActivator();
        final FakeJob job1 = new FakeJob(UUID.randomUUID().toString());
        final AnotherFakeJob job2 = new AnotherFakeJob(UUID.randomUUID().toString());
        activator.register(FakeJob.class, job1);
        activator.register(AnotherFakeJob.class, job2);

        assertThat(
            "activator doesnt return correct instance for second type",
            activator.activateJob(AnotherFakeJob.class),
            is(sameInstance(job2))
        );
    }

    @Test
    void registerOverwritesPreviousInstance() {
        final CacheJobActivator activator = new CacheJobActivator();
        final FakeJob original = new FakeJob(UUID.randomUUID().toString());
        final FakeJob replacement = new FakeJob(UUID.randomUUID().toString());
        activator.register(FakeJob.class, original);
        activator.register(FakeJob.class, replacement);

        assertThat(
            "activator doesnt overwrite previous instance",
            activator.activateJob(FakeJob.class),
            is(sameInstance(replacement))
        );
    }

    /**
     * Fake job class for testing.
     */
    private static final class FakeJob {
        private final String id;

        FakeJob(final String id) {
            this.id = id;
        }
    }

    /**
     * Another fake job class for testing multiple registrations.
     */
    private static final class AnotherFakeJob {
        private final String id;

        AnotherFakeJob(final String id) {
            this.id = id;
        }
    }
}
