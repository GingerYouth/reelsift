package main.java.sift;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** DeepSeek service. */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class DeepSeekService {
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String API_KEY = PropertiesLoader.get("deepSeekApiKey");
    private final String model;
    private final HttpClient httpClient;

    public DeepSeekService() {
        this("deepseek-chat");
    }

    public DeepSeekService(final String model) {
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
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
                System.err.println("DeepSeek API error: " + response.statusCode());
                System.err.println("Response: " + response.body());
                return new ArrayList<>();
            }
        } catch (final Exception e) {
            System.err.println("DeepSeek request failed: " + e.getMessage());
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
        } catch (Exception e) {
            System.err.println("Parsing failed: " + e.getMessage());
            System.err.println("Response: " + jsonResponse);
            return new ArrayList<>();
        }
    }

    private static JSONArray getMoviesArray(String content) {
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
        final JSONArray moviesArray = contentJson.getJSONArray("movies");
        return moviesArray;
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
