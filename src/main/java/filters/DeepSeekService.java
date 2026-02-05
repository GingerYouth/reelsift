package filters;

import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import parser.Movie;
import utils.PropertiesLoader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for getting movie recommendations from DeepSeek AI API.
 * Sends user preferences and available movies to the API and parses
 * the response to filter matching movies.
 */
public class DeepSeekService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeepSeekService.class);
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String API_KEY = PropertiesLoader.get("deepSeekApiKey");
    private final String model;
    private final HttpClient httpClient;

    /**
     * Default constructor using default model.
     */
    public DeepSeekService() {
        this("deepseek-chat");
    }

    /**
     * Constructor with custom model, creates default HttpClient.
     *
     * @param model The model name to use for API calls
     */
    public DeepSeekService(final String model) {
        this(model, HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build());
    }

    /**
     * Primary constructor with dependency injection for testing.
     *
     * @param model The model name to use for API calls
     * @param httpClient HttpClient for making API requests
     */
    DeepSeekService(final String model, final HttpClient httpClient) {
        this.model = model;
        this.httpClient = httpClient;
    }

    public List<Movie> getMovieRecommendations(final String userRequest, final List<Movie> movies) {
        try {
            final JSONArray messages = new JSONArray();
            messages.put(new JSONObject()
                .put("role", "system")
                .put("content", buildSystemPrompt())
            );

            messages.put(new JSONObject()
                .put("role", "user")
                .put("content", userRequest + "\n\n" + moviesAsString(movies))
            );

            final JSONObject requestBody = new JSONObject()
                .put("model", this.model)
                .put("messages", messages)
                .put("temperature", 0.3)
                .put("max_tokens", 1500)
                .put("response_format", new JSONObject().put("type", "json_object"))
                .put("stream", false);

            final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .timeout(Duration.ofSeconds(60))
                .build();

            final HttpResponse<String> response = this.httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 200) {
                return parseResponse(response.body(), movies);
            } else {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("DeepSeek API error: {}. Response: {}", response.statusCode(), response.body());
                }
                return new ArrayList<>();
            }
        } catch (final IOException | InterruptedException | JSONException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("DeepSeek request failed: {}", e.getMessage(), e);
            }
            return new ArrayList<>();
        }
    }

    private static String moviesAsString(final List<Movie> movies) {
        final StringBuilder builder = new StringBuilder("Доступные фильмы:\n");
        for (final Movie movie : movies) {
            builder.append(movie.llmInputFormat());
        }
        return builder.toString();
    }

    @SuppressWarnings("PMD.ExceptionAsFlowControl")
    private static List<Movie> parseResponse(final String jsonResponse, final List<Movie> movies) {
        try {
            final JSONObject responseJson = new JSONObject(jsonResponse);
            if (!responseJson.has("choices")) {
                throw new JSONException("Missing 'choices' in response");
            }
            final JSONArray choices = responseJson.getJSONArray("choices");
            if (choices.isEmpty()) {
                return new ArrayList<>();
            }
            String content = choices.getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
            content = content.replace("\\\"", "\"");

            final JSONArray moviesArray = getMoviesArray(content);
            final List<String> titles = new ArrayList<>();
            for (int i = 0; i < moviesArray.length(); i++) {
                titles.add(moviesArray.getJSONObject(i).getString("title"));
            }
            return movies.stream()
                .filter(m -> titles.contains(m.title()))
                .collect(Collectors.toList());
        } catch (JSONException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Parsing failed: {}. Response: {}", e.getMessage(), jsonResponse, e);
            }
            return new ArrayList<>();
        }
    }

    private static JSONArray getMoviesArray(final String content) {
        JSONObject contentJson;
        try {
            contentJson = new JSONObject(content);
        } catch (final JSONException jsonEx) {
            // Attempt to find JSON substring
            final int startIdx = content.indexOf('{');
            final int endIdx = content.lastIndexOf('}') + 1;
            if (startIdx != -1 && endIdx > startIdx) {
                contentJson = new JSONObject(content.substring(startIdx, endIdx));
            } else {
                throw jsonEx;
            }
        }
        return contentJson.getJSONArray("movies");
    }

    private static String buildSystemPrompt() {
        return String.format(
            """
                Ты кинопомощник. Проанализируй запрос пользователя и порекомендуй подходящие фильмы.
                Инструкции:
                - Отвечай ТОЛЬКО в формате JSON массива с названиями фильмов:
                 "{movies: [{"title": "..."}, {"title": "..."}]}
                - Включай только фильмы, соответствующие запросу.
                - Если ничего не подходит, верни пустой массив.
                - Текущая дата: %s
            """,
            java.time.LocalDate.now()
        );
    }

}
