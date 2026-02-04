package parser;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Parses JSON responses from Afisha API into Session objects. */
public final class SessionJsonParser {

    private SessionJsonParser() {
        // Utility class
    }

    /**
     * Parse sessions from Afisha API JSON response.
     *
     * @param json The JSON string from Afisha API
     * @return List of parsed sessions
     */
    public static List<Session> parseSessions(final String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        final List<Session> result = new ArrayList<>();
        final JSONObject root = new JSONObject(json);
        final JSONObject info = root.getJSONObject("MovieCard").getJSONObject("Info");

        final List<String> genres = extractGenres(info);
        final JSONObject distributorInfo = extractDistributorInfo(info);
        
        final JSONObject scheduleWidget = root.optJSONObject("ScheduleWidget");
        if (scheduleWidget == null) {
            return Collections.emptyList();
        }

        final JSONArray items = scheduleWidget
            .optJSONObject("ScheduleList")
            .optJSONArray("Items");

        if (items == null) {
            return Collections.emptyList();
        }

        for (int i = 0; i < items.length(); i++) {
            final JSONObject item = items.getJSONObject(i);
            final JSONObject place = item.getJSONObject("Place");
            final JSONArray sessions = item.getJSONArray("Sessions");
            result.addAll(createSessionsForPlace(sessions, info, distributorInfo, genres, place));
        }
        return result;
    }

    private static List<String> extractGenres(final JSONObject info) {
        final JSONArray genresArray = info.getJSONObject("Genres").getJSONArray("Links");
        final List<String> genres = new ArrayList<>();
        for (int i = 0; i < genresArray.length(); i++) {
            genres.add(((JSONObject) genresArray.get(i)).get("Name").toString());
        }
        return genres;
    }

    private static JSONObject extractDistributorInfo(final JSONObject info) {
        try {
            return info.getJSONObject("DistributorInfo");
        } catch (final JSONException ignored) {
            return null;
        }
    }

    private static List<Session> createSessionsForPlace(
        final JSONArray sessions,
        final JSONObject info,
        final JSONObject distributorInfo,
        final List<String> genres,
        final JSONObject place
    ) {
        final List<Session> result = new ArrayList<>();
        for (int j = 0; j < sessions.length(); j++) {
            final JSONObject session = sessions.getJSONObject(j);
            result.add(createSession(session, info, distributorInfo, genres, place));
        }
        return result;
    }

    private static Session createSession(
        final JSONObject session,
        final JSONObject info,
        final JSONObject distributorInfo,
        final List<String> genres,
        final JSONObject place
    ) {
        final String price = session.get("MinPriceFormatted").toString();
        return new Session(
            LocalDateTime.parse(session.get("DateTime").toString()),
            info.get("Name").toString(),
            distributorInfo == null ? "" : distributorInfo.get("Text").toString(),
            info.get("Verdict").toString(),
            genres,
            place.getString("Name"),
            place.get("Address").toString(),
            "null".equals(price) ? -1 : Integer.parseInt(price),
            "link",
            "russiansubtitlessession".equals(session.get("SubtitlesFormats").toString()),
            ""
        );
    }
}
