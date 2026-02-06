package filters;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import parser.Movie;
import parser.Session;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for {@link LlmFilter}.
 * Uses a fake DeepSeekService to test filtering logic without network calls.
 */
final class LlmFilterTest {

    @Test
    void filterReturnsEmptyStreamForEmptyInput() {
        final LlmFilter filter = new LlmFilter(
            UUID.randomUUID().toString(),
            new FakeDeepSeekService(Collections.emptyList())
        );

        assertThat(
            "cant return empty stream for empty input",
            filter.filter(Stream.empty()).toList(),
            is(empty())
        );
    }

    @Test
    void filterReturnsSessionsMatchingRecommendedMovies() {
        final String recommendedMovie = "Рекомендованный фильм " + UUID.randomUUID();
        final Session matchingSession = randomSession(recommendedMovie);
        final Session nonMatchingSession = randomSession("Другой фильм");
        final FakeDeepSeekService fakeService = new FakeDeepSeekService(
            List.of(new Movie(recommendedMovie, List.of("Драма"), "Описание", "Вердикт"))
        );
        final LlmFilter filter = new LlmFilter(UUID.randomUUID().toString(), fakeService);

        assertThat(
            "cant return sessions matching recommended movies",
            filter.filter(Stream.of(matchingSession, nonMatchingSession)).toList(),
            contains(matchingSession)
        );
    }

    @Test
    void filterReturnsEmptyWhenNoMoviesMatch() {
        final Session session = randomSession("Фильм А");
        final FakeDeepSeekService fakeService = new FakeDeepSeekService(
            List.of(new Movie("Несуществующий фильм", List.of(), "", ""))
        );
        final LlmFilter filter = new LlmFilter(UUID.randomUUID().toString(), fakeService);

        assertThat(
            "cant return empty list when no sessions match recommendations",
            filter.filter(Stream.of(session)).toList(),
            is(empty())
        );
    }

    @Test
    void filterPreservesAllSessionsOfRecommendedMovie() {
        final String movieName = "Популярный фильм " + UUID.randomUUID();
        final Session session1 = randomSession(movieName);
        final Session session2 = randomSession(movieName);
        final Session session3 = randomSession(movieName);
        final FakeDeepSeekService fakeService = new FakeDeepSeekService(
            List.of(new Movie(movieName, List.of("Комедия"), "", ""))
        );
        final LlmFilter filter = new LlmFilter(UUID.randomUUID().toString(), fakeService);

        assertThat(
            "cant preserve all sessions of recommended movie",
            filter.filter(Stream.of(session1, session2, session3)).toList(),
            hasSize(3)
        );
    }

    @Test
    void filterReturnsEmptyWhenLlmReturnsEmptyList() {
        final Session session = randomSession("Фильм");
        final FakeDeepSeekService fakeService = new FakeDeepSeekService(Collections.emptyList());
        final LlmFilter filter = new LlmFilter(UUID.randomUUID().toString(), fakeService);

        assertThat(
            "cant return empty list when LLM returns no recommendations",
            filter.filter(Stream.of(session)).toList(),
            is(empty())
        );
    }

    @Test
    void filterHandlesMultipleRecommendedMovies() {
        final String movie1 = "Фильм А " + UUID.randomUUID();
        final String movie2 = "Фильм Б " + UUID.randomUUID();
        final Session session1 = randomSession(movie1);
        final Session session2 = randomSession(movie2);
        final Session session3 = randomSession("Не рекомендован");
        final FakeDeepSeekService fakeService = new FakeDeepSeekService(
            List.of(
                new Movie(movie1, List.of(), "", ""),
                new Movie(movie2, List.of(), "", "")
            )
        );
        final LlmFilter filter = new LlmFilter(UUID.randomUUID().toString(), fakeService);

        assertThat(
            "cant return sessions for multiple recommended movies",
            filter.filter(Stream.of(session1, session2, session3)).toList(),
            hasSize(2)
        );
    }

    @Test
    void filterPassesFormattedRequestToService() {
        final String userRequest = "комедию с хорошим рейтингом";
        final List<String> capturedRequests = new ArrayList<>();
        final FakeDeepSeekService fakeService = new FakeDeepSeekService(Collections.emptyList()) {
            @Override
            public List<Movie> getMovieRecommendations(
                final String request, final List<Movie> movies
            ) {
                capturedRequests.add(request);
                return super.getMovieRecommendations(request, movies);
            }
        };
        final LlmFilter filter = new LlmFilter(userRequest, fakeService);
        filter.filter(Stream.of(randomSession("Фильм"))).toList();

        assertThat(
            "cant pass user request to DeepSeek service",
            capturedRequests.get(0).contains(userRequest),
            is(true)
        );
    }

    private static Session randomSession(final String movieName) {
        return new Session(
            LocalDateTime.now().plusDays(new Random().nextInt(30)),
            movieName,
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            List.of("Драма"),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            new Random().nextInt(1000),
            UUID.randomUUID().toString(),
            new Random().nextBoolean()
        );
    }

    /**
     * Fake DeepSeekService that returns preconfigured recommendations.
     */
    private static class FakeDeepSeekService extends DeepSeekService {
        private final List<Movie> recommendations;

        FakeDeepSeekService(final List<Movie> recommendations) {
            super("test-model");
            this.recommendations = recommendations;
        }

        @Override
        public List<Movie> getMovieRecommendations(
            final String userRequest, final List<Movie> movies
        ) {
            return movies.stream()
                .filter(m -> this.recommendations.stream()
                    .anyMatch(rec -> rec.title().equals(m.title())))
                .toList();
        }
    }
}
