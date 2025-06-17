package main.java.sift;

import main.java.sift.filters.ExcludedGenres;
import main.java.sift.filters.Filters;
import main.java.sift.filters.MandatoryGenres;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static main.java.sift.AfishaParser.parseTodayFilms;

/** Main class. */
@SuppressWarnings({"PMD.ShortClassName", "PMD.SignatureDeclareThrowsException"})
public class Main {
    public static void main(String[]args) throws Exception {
        final AfishaParser parser = new AfishaParser();
        final Map<String, String> films = parseTodayFilms();
        final List<Session> sessions = new ArrayList<>();
        for (final Map.Entry<String, String> entry : films.entrySet()) {
            sessions.addAll(parser.parseSchedule(entry.getValue()));
        }
        //final TimeFilter timeFilter = new TimeFilter("15:50", "23:59");
        final ExcludedGenres excluded = new ExcludedGenres(List.of("Комедия", "Ужасы"));
        final MandatoryGenres mandatory = new MandatoryGenres(List.of(
            "Триллер", "Драма", "Фантастика", "Приключение", "Детектив", "Исторический"
        ));
        //final LlmFilter llmFilter = new LlmFilter("фильм про человеский подвиг");
        final Filters filters = new Filters(
            //timeFilter,
            excluded,
            mandatory
            //llmFilter
        );
        final List<Session> filtered = filters.filter(sessions);
        print(filtered);
    }

    public static void print(final List<Session> sessions) {
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
                MovieGroup::filmName,
                String.CASE_INSENSITIVE_ORDER
            ))
            .toList();

        for (final MovieGroup group : movieGroups) {
            System.out.println("Film: " + group.filmName());
            System.out.print("Genres: ");
            System.out.println(
                group.genres()
                    .stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", "))
            );
            System.out.println("Verdict: " + group.verdict());
            System.out.println("Description: " + group.description());
            System.out.println("Sessions: ");

            for (final Session session : group.sessions()) {
                System.out.printf(
                    "  %s at %s (Address: %s), Price: %d RUB, Link: %s%n",
                    session.dateTime(),
                    session.cinema(),
                    session.address(),
                    session.price(),
                    session.link()
                );
            }
            System.out.println();
        }
    }

    private record MovieGroup(
        String filmName, List<String> genres, String verdict, String description, List<Session> sessions
    ) {}
}