package bots.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.Test;
import parser.Session;

/**
 * Unit tests for {@link SessionCallbackStore}.
 */
public class SessionCallbackStoreTest {

    @Test
    public void storeAndRetrieveReturnsStoredSessions() {
        final SessionCallbackStore store = new SessionCallbackStore();
        final String cinema = "Cinema " + UUID.randomUUID();
        final List<Session> sessions = List.of(
            new Session(
                LocalDateTime.of(2024, 1, 1, 10, 0),
                "Film", "Desc", "Verdict",
                List.of("Drama"), cinema, "Addr", 500, "link", false
            )
        );
        final String callbackId = store.store(42, "12345", sessions);
        final Optional<SessionCallbackStore.StoredCallback> result =
            store.retrieve(callbackId);
        assertThat(
            "cant retrieve stored sessions by callback ID",
            result.isPresent(),
            is(true)
        );
    }

    @Test
    public void retrieveUnknownIdReturnsEmpty() {
        final SessionCallbackStore store = new SessionCallbackStore();
        assertThat(
            "cant return empty for unknown callback ID",
            store.retrieve("unknown1"),
            is(Optional.empty())
        );
    }

    @Test
    public void storeReturnsDifferentIdsForDifferentCalls() {
        final SessionCallbackStore store = new SessionCallbackStore();
        final List<Session> sessions = List.of(
            new Session(
                LocalDateTime.of(2024, 1, 1, 10, 0),
                "Film", "Desc", "Verdict",
                List.of("Drama"), "Cinema", "Addr", 500, "link", false
            )
        );
        final String firstId = store.store(1, "111", sessions);
        final String secondId = store.store(2, "222", sessions);
        assertThat(
            "cant return different IDs for different store calls",
            firstId,
            is(not(secondId))
        );
    }

    @Test
    public void retrieveRemovesEntryFromStore() {
        final SessionCallbackStore store = new SessionCallbackStore();
        final List<Session> sessions = List.of(
            new Session(
                LocalDateTime.of(2024, 1, 1, 10, 0),
                "Film", "Desc", "Verdict",
                List.of("Drama"), "Cinema", "Addr", 500, "link", false
            )
        );
        final String callbackId = store.store(42, "12345", sessions);
        store.retrieve(callbackId);
        assertThat(
            "cant remove entry after retrieval",
            store.retrieve(callbackId),
            is(Optional.empty())
        );
    }

    @Test
    public void storedCallbackContainsCorrectMessageId() {
        final SessionCallbackStore store = new SessionCallbackStore();
        final List<Session> sessions = List.of(
            new Session(
                LocalDateTime.of(2024, 1, 1, 10, 0),
                "Film", "Desc", "Verdict",
                List.of("Drama"), "Cinema", "Addr", 500, "link", false
            )
        );
        final String callbackId = store.store(99, "12345", sessions);
        assertThat(
            "cant preserve message ID in stored callback",
            store.retrieve(callbackId).orElseThrow().messageId(),
            is(99)
        );
    }

    @Test
    public void storedCallbackContainsCorrectSessionCount() {
        final SessionCallbackStore store = new SessionCallbackStore();
        final List<Session> sessions = List.of(
            new Session(
                LocalDateTime.of(2024, 1, 1, 10, 0),
                "Film", "Desc", "Verdict",
                List.of("Drama"), "Cinema", "Addr", 500, "link", false
            ),
            new Session(
                LocalDateTime.of(2024, 1, 1, 12, 0),
                "Film", "Desc", "Verdict",
                List.of("Drama"), "Cinema2", "Addr2", 600, "link2", false
            )
        );
        final String callbackId = store.store(42, "12345", sessions);
        assertThat(
            "cant preserve session count in stored callback",
            store.retrieve(callbackId).orElseThrow().sessions(),
            hasSize(2)
        );
    }
}
