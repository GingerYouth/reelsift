package main.java.sift;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import static main.java.sift.AfishaParser.parseSchedulePage;
import static main.java.sift.AfishaParser.parseTodayFilms;

/** Main class. */
public class Main {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static void main(String[]args) throws Exception {
        final Map<String, String> films = parseTodayFilms();
        parseSchedulePage("");
        int x = 0;
    }
}