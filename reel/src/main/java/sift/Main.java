package main.java.sift;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import main.java.sift.filters.ExcludedGenres;
import main.java.sift.filters.Filters;
import main.java.sift.filters.MandatoryGenres;

/** Main class. */
@SuppressWarnings({"PMD.ShortClassName", "PMD.SignatureDeclareThrowsException"})
public class Main {
    public static void main(String[]args) throws Exception {
        final AfishaParser parser = new AfishaParser();
        final Map<String, String> films = AfishaParser.parseTodayFilms();
        final List<Session> sessions = new ArrayList<>();
        for (final Map.Entry<String, String> entry : films.entrySet()) {
            sessions.addAll(parser.parseSchedule(entry.getValue()));
        }
        //final TimeFilter timeFilter = new TimeFilter("15:50", "23:59");
        final ExcludedGenres excluded = new ExcludedGenres(List.of("Комедия", "Ужасы"));
        final MandatoryGenres mandatory = new MandatoryGenres(List.of("Триллер"));
        //final LlmFilter llmFilter = new LlmFilter("фильм про человеский подвиг");
        final Filters filters = new Filters(
            //timeFilter,
            excluded,
            mandatory
        );
        final List<Session> filtered = filters.filter(sessions);
        System.out.println(Session.toString(filtered));
    }
}