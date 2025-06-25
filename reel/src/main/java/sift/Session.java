package main.java.sift;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Session DTO. */
@SuppressWarnings("PMD.ConsecutiveAppendsShouldReuse")
public record Session(
    LocalTime dateTime, String name, String description, String verdict,
    List<String> genres, String cinema, String address, int price, String link) {

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
            result.add(builder.toString());
            builder.setLength(0);

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
                result.add(builder.toString());
                builder.setLength(0);
            }
        }
        return result;
    }
}
