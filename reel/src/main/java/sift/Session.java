package main.java.sift;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.json.JSONArray;
import org.json.JSONObject;

/** Session DTO. */
@SuppressWarnings("PMD.ConsecutiveLiteralAppends")
public record Session(
    LocalDateTime dateTime, String name, String description, String verdict,
    List<String> genres, String cinema, String address, int price, String link
) {

    public static String toJson(final List<Session> sessions) {
        final JSONArray jsonArray = new JSONArray();
        jsonArray.putAll(sessions.stream().map(Session::toJson).collect(Collectors.toList()));
        return jsonArray.toString();
    }

    private String toJson() {
        final JSONObject obj = new JSONObject();
        obj.put("dateTime", this.dateTime().toString());
        obj.put("name", this.name());
        obj.put("description", this.description());
        obj.put("verdict", this.verdict());
        obj.put("genres", this.genres());
        obj.put("cinema", this.cinema());
        obj.put("address", this.address());
        obj.put("price", this.price());
        obj.put("link", this.link());
        return obj.toString();
    }

    public static List<Session> fromJsonArray(final String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        if (json.charAt(0) == '[') {
            final JSONArray arr = new JSONArray(json);
            return IntStream.range(0, arr.length())
                .mapToObj(i -> fromJson(arr.get(i).toString()))
                .collect(Collectors.toList());
        } else if (json.charAt(0) == '{') {
            return Collections.singletonList(fromJson(json));
        } else {
            // TODO:: Log it
            return Collections.emptyList();
        }
    }

    private static Session fromJson(final String json) {
        final JSONObject obj = new JSONObject(json);
        final LocalDateTime dateTime = LocalDateTime.parse(obj.getString("dateTime"));
        final String name = obj.getString("name");
        final String description = obj.getString("description");
        final String verdict = obj.getString("verdict");
        final List<String> genres = obj.getJSONArray("genres").toList().stream().map(Object::toString).toList();
        final String cinema = obj.getString("cinema");
        final String address = obj.getString("address");
        final int price = obj.getInt("price");
        final String link = obj.getString("link");
        return new Session(dateTime, name, description, verdict, genres, cinema, address, price, link);
    }

    /**
     * Group of sessions of specific movie.
     */
    public record MovieGroup(
        String movieName, List<String> genres, String verdict, String description, List<Session> sessions
    ) {}

    public static String toString(final List<Session> sessions) {
        final StringBuilder builder = new StringBuilder(60);

        final Map<String, List<Session>> groupedByName = sessions.stream()
            .collect(Collectors.groupingBy(Session::name));

        final List<MovieGroup> movieGroups = groupedByName.entrySet().stream()
            .map(entry -> {
                final Session firstSession = entry.getValue().getFirst();
                // Sort sessions by time
                final List<Session> sortedSessions = entry.getValue().stream()
                    .sorted(Comparator.comparing(Session::dateTime))
                    .collect(Collectors.toList());
                return new MovieGroup(
                    entry.getKey(), firstSession.genres(),
                    firstSession.verdict(), firstSession.description(),
                    sortedSessions);
            })
            .sorted(Comparator.comparing(
                MovieGroup::movieName,
                String.CASE_INSENSITIVE_ORDER
            ))
            .toList();

        for (final MovieGroup group : movieGroups) {
            builder.append("Film: ").append(group.movieName()).append('\n')
                .append("Genres: ")
                .append(
                    group.genres()
                        .stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(", "))
                )
                .append("Verdict: ").append(group.verdict()).append('\n')
                .append("Description: ").append(group.description).append('\n')
                .append("Sessions: ").append('\n');
            for (final Session session : group.sessions()) {
                builder.append(
                    String.format(
                        "  %s at %s (Address: %s), Price: %d RUB, Link: %s%n",
                        session.dateTime(),
                        session.cinema(),
                        session.address(),
                        session.price(),
                        session.link()
                    )
                );
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    public static List<String> toSplitStrings(final List<Session> sessions) {
        final List<String> result = new ArrayList<>();
        final StringBuilder builder = new StringBuilder(60);

        final Map<String, List<Session>> groupedByName = sessions.stream()
            .collect(Collectors.groupingBy(Session::name));

        final List<MovieGroup> movieGroups = groupedByName.entrySet().stream()
            .map(entry -> {
                final Session firstSession = entry.getValue().getFirst();
                // Sort sessions by time
                final List<Session> sortedSessions = entry.getValue().stream()
                    .sorted(Comparator.comparing(Session::dateTime))
                    .collect(Collectors.toList());
                return new MovieGroup(
                    entry.getKey(), firstSession.genres(),
                    firstSession.verdict(), firstSession.description(),
                    sortedSessions);
            })
            .sorted(Comparator.comparing(
                MovieGroup::movieName,
                String.CASE_INSENSITIVE_ORDER
            ))
            .toList();
        result.add(builder.toString());
        builder.setLength(0);

        for (final MovieGroup group : movieGroups) {
            builder.append("<b>Фильм:</b> ").append(group.movieName()).append('\n')
                .append("<b>Жанры:</b> ")
                .append(
                    group.genres()
                        .stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(", "))
                )
                .append("\n<b>О чём:</b> ").append(group.verdict()).append('\n')
                .append("<b>Описание:</b> ").append(group.description).append('\n')
                .append("<b>Сеансы:</b> ").append('\n');
            result.add(builder.toString());
            builder.setLength(0);

            for (final Session session : group.sessions()) {
                builder.append(
                    String.format(
                        "  %s в %s (Адрес: <i>%s</i>), Цена: %d RUB",
                        session.dateTime(),
                        session.cinema(),
                        session.address(),
                        session.price()
                    )
                );
                result.add(builder.toString());
                builder.setLength(0);
            }
        }
        return result;
    }
}
