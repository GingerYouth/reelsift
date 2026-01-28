package filters;

import parser.Movie;
import parser.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * LLM-based filter that uses AI recommendations to filter sessions.
 * Sends movie information to DeepSeek API and returns sessions matching
 * recommended movies.
 */
public final class LlmFilter implements Filter {
    private static final String BASE =
        """
            Я хочу сходить в кинотеатр и посмотреть %s
            Помоги мне выбрать фильм из представленных:
        """;

    private final String request;
    private final DeepSeekService deepSeekService;

    /**
     * Primary constructor with dependency injection.
     *
     * @param request User's movie preference request
     * @param deepSeekService Service for getting AI recommendations
     */
    public LlmFilter(final String request, final DeepSeekService deepSeekService) {
        this.request = String.format(BASE, request);
        this.deepSeekService = deepSeekService;
    }

    /**
     * Secondary constructor using default DeepSeekService.
     *
     * @param request User's movie preference request
     */
    public LlmFilter(final String request) {
        this(request, new DeepSeekService());
    }

    @Override
    public Stream<Session> filter(final Stream<Session> shows) {
        final List<Session> showList = shows.toList();
        final Map<String, Movie> movieMap = showList.stream().collect(Collectors.toMap(
            Session::name,
            s -> new Movie(s.name(), s.genres(), s.description(), s.verdict()),
            (existing, ignore) -> existing
        ));
        final List<Movie> movies = new ArrayList<>(movieMap.values());
        if (movies.isEmpty()) {
            return Stream.empty();
        }
        final List<Movie> recommendedMovies = this.deepSeekService.getMovieRecommendations(this.request, movies);
        final Set<String> recommendedTitles = recommendedMovies
            .stream()
            .map(Movie::title)
            .collect(Collectors.toSet());
        return showList.stream().filter(s -> recommendedTitles.contains(s.name()));
    }
}
