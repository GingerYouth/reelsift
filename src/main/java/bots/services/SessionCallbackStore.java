package bots.services;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import parser.Session;

/**
 * In-memory store for session callback data, keyed by short UUID-based IDs.
 * Used to map inline button callbacks to their associated session lists.
 */
public final class SessionCallbackStore {

    private final ConcurrentMap<String, StoredCallback> callbacks;

    /**
     * Creates a new empty callback store.
     */
    public SessionCallbackStore() {
        this.callbacks = new ConcurrentHashMap<>();
    }

    /**
     * A stored callback entry containing the original message ID,
     * chat ID, and the list of sessions to display.
     *
     * @param messageId The Telegram message ID of the film message
     * @param chatId The chat ID where the message was sent
     * @param sessions The sessions to display when the button is pressed
     */
    public record StoredCallback(
        int messageId, String chatId, List<Session> sessions
    ) {}

    /**
     * Stores a callback entry and returns a short callback ID.
     *
     * @param messageId The Telegram message ID
     * @param chatId The chat ID
     * @param sessions The sessions to store
     * @return An 8-character hex callback ID
     */
    public String store(
        final int messageId,
        final String chatId,
        final List<Session> sessions
    ) {
        final String callbackId = UUID.randomUUID()
            .toString().substring(0, 8);
        this.callbacks.put(
            callbackId,
            new StoredCallback(messageId, chatId, sessions)
        );
        return callbackId;
    }

    /**
     * Retrieves and removes a stored callback by its ID.
     *
     * @param callbackId The callback ID returned by {@link #store}
     * @return The stored callback, or empty if not found
     */
    public Optional<StoredCallback> retrieve(final String callbackId) {
        return Optional.ofNullable(this.callbacks.remove(callbackId));
    }
}
