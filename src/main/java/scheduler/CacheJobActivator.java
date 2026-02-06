package scheduler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jobrunr.server.JobActivator;

/**
 * Custom job activator for JobRunr when no dependency injection framework is used.
 * Stores instances by their class type and returns them when JobRunr needs to execute a job.
 */
public final class CacheJobActivator implements JobActivator {

    private final Map<Class<?>, Object> instances;

    /**
     * Creates an empty job activator.
     */
    public CacheJobActivator() {
        this.instances = new ConcurrentHashMap<>();
    }

    /**
     * Registers an instance for a given type.
     *
     * @param <T> The type of the instance
     * @param type The class type to register
     * @param instance The instance to return when this type is requested
     */
    public <T> void register(final Class<T> type, final T instance) {
        this.instances.put(type, instance);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T activateJob(final Class<T> type) {
        final Object instance = this.instances.get(type);
        if (instance == null) {
            throw new IllegalStateException(
                String.format("No instance registered for type %s", type.getName())
            );
        }
        return (T) instance;
    }
}
