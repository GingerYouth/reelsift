package filters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import parser.Movie;

/**
 * Unit tests for {@link DeepSeekService}.
 * Uses a fake HttpClient to test API interaction without network calls.
 */
final class DeepSeekServiceTest {

    @Test
    void getMovieRecommendationsReturnsEmptyListOnApiError500() {
        final HttpClient fakeClient = new FakeHttpClient(500, "Internal Server Error");
        final DeepSeekService service = new DeepSeekService("test-model", fakeClient);
        final List<Movie> movies = List.of(
            new Movie("Фильм", List.of("Драма"), "Описание", "Вердикт")
        );

        assertThat(
            "cant return empty list when API returns 500 error",
            service.getMovieRecommendations(UUID.randomUUID().toString(), movies),
            is(empty())
        );
    }

    @Test
    void getMovieRecommendationsReturnsMatchingMoviesOnSuccess() {
        final String movieTitle = "Рекомендованный фильм";
        final String apiResponse = buildApiResponse(List.of(movieTitle));
        final HttpClient fakeClient = new FakeHttpClient(200, apiResponse);
        final DeepSeekService service = new DeepSeekService("test-model", fakeClient);
        final Movie matchingMovie = new Movie(movieTitle, List.of("Драма"), "", "");
        final Movie otherMovie = new Movie("Другой фильм", List.of("Комедия"), "", "");

        final List<Movie> result = service.getMovieRecommendations(
            UUID.randomUUID().toString(),
            List.of(matchingMovie, otherMovie)
        );

        assertThat(
            "cant return matching movies from API response",
            result,
            contains(matchingMovie)
        );
    }

    @Test
    void getMovieRecommendationsReturnsEmptyListOnConnectionException() {
        final HttpClient fakeClient = new ExceptionThrowingHttpClient(
            new IOException("Connection refused")
        );
        final DeepSeekService service = new DeepSeekService("test-model", fakeClient);
        final List<Movie> movies = List.of(
            new Movie("Фильм", List.of(), "", "")
        );

        assertThat(
            "cant return empty list when connection fails",
            service.getMovieRecommendations(UUID.randomUUID().toString(), movies),
            is(empty())
        );
    }

    @Test
    void getMovieRecommendationsReturnsEmptyListOnMalformedJson() {
        final HttpClient fakeClient = new FakeHttpClient(200, "not valid json {{{");
        final DeepSeekService service = new DeepSeekService("test-model", fakeClient);
        final List<Movie> movies = List.of(
            new Movie("Фильм", List.of(), "", "")
        );

        assertThat(
            "cant return empty list when response is malformed JSON",
            service.getMovieRecommendations(UUID.randomUUID().toString(), movies),
            is(empty())
        );
    }

    @Test
    void getMovieRecommendationsReturnsEmptyListWhenChoicesMissing() {
        final String responseWithoutChoices = "{\"id\": \"123\", \"object\": \"chat.completion\"}";
        final HttpClient fakeClient = new FakeHttpClient(200, responseWithoutChoices);
        final DeepSeekService service = new DeepSeekService("test-model", fakeClient);
        final List<Movie> movies = List.of(
            new Movie("Фильм", List.of(), "", "")
        );

        assertThat(
            "cant return empty list when response lacks choices field",
            service.getMovieRecommendations(UUID.randomUUID().toString(), movies),
            is(empty())
        );
    }

    @Test
    void getMovieRecommendationsReturnsOnlyInputMoviesThatMatchResponse() {
        final String movieInResponse = "Фильм А";
        final String movieNotInInput = "Фильм Б";
        final String apiResponse = buildApiResponse(List.of(movieInResponse, movieNotInInput));
        final HttpClient fakeClient = new FakeHttpClient(200, apiResponse);
        final DeepSeekService service = new DeepSeekService("test-model", fakeClient);
        final Movie inputMovie = new Movie(movieInResponse, List.of(), "", "");

        final List<Movie> result = service.getMovieRecommendations(
            UUID.randomUUID().toString(),
            List.of(inputMovie)
        );

        assertThat(
            "cant return only movies from input list that match response",
            result,
            hasSize(1)
        );
    }

    @Test
    void getMovieRecommendationsReturnsEmptyListWhenChoicesArrayEmpty() {
        final String responseWithEmptyChoices = "{\"choices\": []}";
        final HttpClient fakeClient = new FakeHttpClient(200, responseWithEmptyChoices);
        final DeepSeekService service = new DeepSeekService("test-model", fakeClient);
        final List<Movie> movies = List.of(
            new Movie("Фильм", List.of(), "", "")
        );

        assertThat(
            "cant return empty list when choices array is empty",
            service.getMovieRecommendations(UUID.randomUUID().toString(), movies),
            is(empty())
        );
    }

    @Test
    void getMovieRecommendationsHandlesMultipleRecommendedMovies() {
        final String movie1 = "Фильм А " + UUID.randomUUID();
        final String movie2 = "Фильм Б " + UUID.randomUUID();
        final String apiResponse = buildApiResponse(List.of(movie1, movie2));
        final HttpClient fakeClient = new FakeHttpClient(200, apiResponse);
        final DeepSeekService service = new DeepSeekService("test-model", fakeClient);

        final List<Movie> result = service.getMovieRecommendations(
            UUID.randomUUID().toString(),
            List.of(
                new Movie(movie1, List.of(), "", ""),
                new Movie(movie2, List.of(), "", ""),
                new Movie("Не рекомендован", List.of(), "", "")
            )
        );

        assertThat(
            "cant return multiple recommended movies",
            result,
            hasSize(2)
        );
    }

    @Test
    void getMovieRecommendationsReturnsEmptyListOnInterruptedException() {
        final HttpClient fakeClient = new ExceptionThrowingHttpClient(
            new InterruptedException("Thread interrupted")
        );
        final DeepSeekService service = new DeepSeekService("test-model", fakeClient);
        final List<Movie> movies = List.of(
            new Movie("Фильм", List.of(), "", "")
        );

        assertThat(
            "cant return empty list when thread is interrupted",
            service.getMovieRecommendations(UUID.randomUUID().toString(), movies),
            is(empty())
        );
    }

    private static String buildApiResponse(final List<String> movieTitles) {
        final StringBuilder movies = new StringBuilder();
        for (int i = 0; i < movieTitles.size(); i++) {
            if (i > 0) {
                movies.append(", ");
            }
            movies.append("{\\\"title\\\": \\\"").append(movieTitles.get(i)).append("\\\"}");
        }
        return String.format(
            "{\"choices\": [{\"message\": {\"content\": \"{\\\"movies\\\": [%s]}\"}}]}",
            movies
        );
    }

    /**
     * Fake HttpClient that returns preconfigured responses.
     */
    private static final class FakeHttpClient extends HttpClient {
        private final int statusCode;
        private final String body;

        FakeHttpClient(final int statusCode, final String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> HttpResponse<T> send(
            final HttpRequest request,
            final HttpResponse.BodyHandler<T> responseBodyHandler
        ) {
            return new FakeHttpResponse<>(this.statusCode, (T) this.body);
        }

        @Override
        public java.util.Optional<java.net.CookieHandler> cookieHandler() {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<java.time.Duration> connectTimeout() {
            return java.util.Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public java.util.Optional<java.net.ProxySelector> proxy() {
            return java.util.Optional.empty();
        }

        @Override
        public javax.net.ssl.SSLContext sslContext() {
            return null;
        }

        @Override
        public javax.net.ssl.SSLParameters sslParameters() {
            return null;
        }

        @Override
        public java.util.Optional<java.util.concurrent.Executor> executor() {
            return java.util.Optional.empty();
        }

        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }

        @Override
        public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
            final HttpRequest request,
            final HttpResponse.BodyHandler<T> responseBodyHandler
        ) {
            return java.util.concurrent.CompletableFuture.completedFuture(
                send(request, responseBodyHandler)
            );
        }

        @Override
        public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
            final HttpRequest request,
            final HttpResponse.BodyHandler<T> responseBodyHandler,
            final HttpResponse.PushPromiseHandler<T> pushPromiseHandler
        ) {
            return sendAsync(request, responseBodyHandler);
        }

        @Override
        public java.util.Optional<java.net.Authenticator> authenticator() {
            return java.util.Optional.empty();
        }
    }

    /**
     * Fake HttpResponse for testing.
     */
    private static final class FakeHttpResponse<T> implements HttpResponse<T> {
        private final int statusCode;
        private final T body;

        FakeHttpResponse(final int statusCode, final T body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        @Override
        public int statusCode() {
            return this.statusCode;
        }

        @Override
        public T body() {
            return this.body;
        }

        @Override
        public HttpRequest request() {
            return null;
        }

        @Override
        public java.util.Optional<HttpResponse<T>> previousResponse() {
            return java.util.Optional.empty();
        }

        @Override
        public java.net.http.HttpHeaders headers() {
            return java.net.http.HttpHeaders.of(java.util.Map.of(), (a, b) -> true);
        }

        @Override
        public java.util.Optional<javax.net.ssl.SSLSession> sslSession() {
            return java.util.Optional.empty();
        }

        @Override
        public java.net.URI uri() {
            return null;
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }

    /**
     * Fake HttpClient that throws exceptions.
     */
    private static final class ExceptionThrowingHttpClient extends HttpClient {
        private final Exception exception;

        ExceptionThrowingHttpClient(final Exception exception) {
            this.exception = exception;
        }

        @Override
        public <T> HttpResponse<T> send(
            final HttpRequest request,
            final HttpResponse.BodyHandler<T> responseBodyHandler
        ) throws IOException, InterruptedException {
            if (this.exception instanceof IOException ioe) {
                throw ioe;
            } else if (this.exception instanceof InterruptedException ie) {
                throw ie;
            }
            throw new RuntimeException(this.exception);
        }

        @Override
        public java.util.Optional<java.net.CookieHandler> cookieHandler() {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<java.time.Duration> connectTimeout() {
            return java.util.Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public java.util.Optional<java.net.ProxySelector> proxy() {
            return java.util.Optional.empty();
        }

        @Override
        public javax.net.ssl.SSLContext sslContext() {
            return null;
        }

        @Override
        public javax.net.ssl.SSLParameters sslParameters() {
            return null;
        }

        @Override
        public java.util.Optional<java.util.concurrent.Executor> executor() {
            return java.util.Optional.empty();
        }

        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }

        @Override
        public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
            final HttpRequest request,
            final HttpResponse.BodyHandler<T> responseBodyHandler
        ) {
            return java.util.concurrent.CompletableFuture.failedFuture(this.exception);
        }

        @Override
        public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
            final HttpRequest request,
            final HttpResponse.BodyHandler<T> responseBodyHandler,
            final HttpResponse.PushPromiseHandler<T> pushPromiseHandler
        ) {
            return sendAsync(request, responseBodyHandler);
        }

        @Override
        public java.util.Optional<java.net.Authenticator> authenticator() {
            return java.util.Optional.empty();
        }
    }
}
