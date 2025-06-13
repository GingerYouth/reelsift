package main.java.sift;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static main.java.sift.AfishaParser.parseTodayFilms;

/** Main class. */
public class Main {
    public static void main(String[]args) throws Exception {
        final AfishaParser parser = new AfishaParser();
        final Map<String, String> films = parseTodayFilms();
        final List<Session> sessions = new ArrayList<>();
        for (final Map.Entry<String, String> entry : films.entrySet()) {
            sessions.addAll(parser.parseSchedule(entry.getValue()));
        }
        // TODO:: Sessions filtering
        // TODO:: Output
        System.out.println("");
    }
}