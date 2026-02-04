package parser;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Session DTO.
 */
@SuppressWarnings({"PMD.ConsecutiveLiteralAppends", "PMD.TooManyMethods"})
public final class Session {
    private final LocalDateTime dateTime;
    private final String name;
    private final String description;
    private final String verdict;
    private final List<String> genres;
    private final String cinema;
    private final String address;
    private final int price;
    private final String link;
    private final boolean russianSubtitlesSession;
    private String imageUrl;

    /** Constructor. */
    public Session(
        final LocalDateTime dateTime, final String name,
        final String description, final String verdict,
        final List<String> genres, final String cinema,
        final String address, final int price,
        final String link, final boolean russianSubtitlesSession,
        final String imageUrl
    ) {
        this.dateTime = dateTime;
        this.name = name;
        this.description = description;
        this.verdict = verdict;
        this.genres = genres;
        this.cinema = cinema;
        this.address = address;
        this.price = price;
        this.link = link;
        this.russianSubtitlesSession = russianSubtitlesSession;
        this.imageUrl = imageUrl;
    }

    /** Constructor. */
    public Session(
        final LocalDateTime dateTime, final String name,
        final String description, final String verdict,
        final List<String> genres, final String cinema,
        final String address, final int price,
        final String link, final boolean withSubs
    ) {
        this(
            dateTime, name, description, verdict, genres,
            cinema, address, price, link, withSubs, ""
        );
    }

    public void setImageUrl(final String imageUrl) {
        this.imageUrl = imageUrl;
    }

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
        obj.put("imageUrl", this.imageUrl());
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
        final boolean withSubs = obj.optBoolean("russianSubtitlesSession", false);
        final String imageUrl = obj.getString("imageUrl");
        return new Session(
            dateTime, name, description, verdict, genres, cinema,
            address, price, link, withSubs, imageUrl
        );
    }

    /**
     * Group of sessions of specific movie.
     */
    public record MovieGroup(
        String movieName, List<String> genres, String verdict,
        String description, List<Session> sessions, String imageUrl
    ) {
    }

    /**
     * A message for a single film, containing the formatted text
     * and any sessions that were omitted due to the threshold.
     */
    public record FilmMessage(
        String text, List<Session> omittedSessions, String imageUrl
    ) {
    }

    /**
     * Formats sessions as a plain-text string grouped by movie.
     * If a film has {@code sessionThreshold} or more sessions,
     * only film info is shown without individual session lines.
     *
     * @param sessions         List of sessions to format
     * @param sessionThreshold Max sessions per film before they're omitted
     * @return Plain-text formatted string
     */
    public static String toString(
        final List<Session> sessions, final int sessionThreshold
    ) {
        final StringBuilder builder = new StringBuilder(60);

        final Map<String, List<Session>> groupedByName = sessions.stream()
                .collect(Collectors.groupingBy(Session::name));

        final List<MovieGroup> movieGroups = groupedByName.entrySet().stream()
            .map(entry -> {
                final Session firstSession = entry.getValue().getFirst();
                final List<Session> sortedSessions = entry.getValue().stream()
                    .sorted(Comparator.comparing(Session::dateTime))
                    .collect(Collectors.toList());
                return new MovieGroup(
                    entry.getKey(), firstSession.genres(),
                    firstSession.verdict(), firstSession.description(),
                    sortedSessions, firstSession.imageUrl());
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
                .append("Description: ").append(group.description).append('\n');
            if (group.sessions().size() < sessionThreshold) {
                builder.append("Sessions: ").append('\n');
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
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    /**
     * Formats sessions as a list of {@link FilmMessage} records, one per movie.
     * Sessions for one film are combined into a single message.
     * If a film has {@code sessionThreshold} or more sessions,
     * only film info is shown without individual session lines,
     * and the omitted sessions are included in the result.
     *
     * @param sessions         List of sessions to format
     * @param sessionThreshold Max sessions per film before they're omitted
     * @return List of {@link FilmMessage} records, one per movie
     */
    public static List<FilmMessage> toSplitStrings(
            final List<Session> sessions, final int sessionThreshold
    ) {
        final List<FilmMessage> result = new ArrayList<>();
        final StringBuilder builder = new StringBuilder(60);

        final Map<String, List<Session>> groupedByName = sessions.stream()
                .collect(Collectors.groupingBy(Session::name));

        final List<MovieGroup> movieGroups = groupedByName.entrySet().stream()
                .map(entry -> {
                    final Session firstSession = entry.getValue().getFirst();
                    final List<Session> sortedSessions = entry.getValue().stream()
                        .sorted(Comparator.comparing(Session::dateTime))
                        .collect(Collectors.toList());
                    return new MovieGroup(
                        entry.getKey(), firstSession.genres(),
                        firstSession.verdict(), firstSession.description(),
                        sortedSessions, firstSession.imageUrl()); // Added imageUrl
                })
                .sorted(Comparator.comparing(
                        MovieGroup::movieName,
                        String.CASE_INSENSITIVE_ORDER
                ))
                .toList();

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
                .append("<b>Описание:</b> ").append(group.description).append('\n');

            // Always add the message with the sessions omitted from the text and placed in the omittedSessions list.
            result.add(
                new FilmMessage(
                    builder.toString(),
                    group.sessions(),
                    group.imageUrl()
                )
            );
            builder.setLength(0);
        }
        return result;
    }

    /**
     * Formats a list of sessions as HTML session lines.
     * Each line contains the date/time, cinema name, address, and price.
     *
     * @param sessions List of sessions to format
     * @return HTML-formatted string with session details
     */
    public static String formatSessionLines(final List<Session> sessions) {
        final StringBuilder builder = new StringBuilder(60);
        builder.append("<b>Сеансы:</b>\n");
        for (final Session session : sessions) {
            builder.append(
                    String.format(
                            "  %s в %s (Адрес: <i>%s</i>), Цена: %d RUB\n",
                            session.dateTime(),
                            session.cinema(),
                            session.address(),
                            session.price()
                    )
            );
        }
        return builder.toString();
    }

    public LocalDateTime dateTime() {
        return dateTime;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public String verdict() {
        return verdict;
    }

    public List<String> genres() {
        return genres;
    }

    public String cinema() {
        return cinema;
    }

    public String address() {
        return address;
    }

    public int price() {
        return price;
    }

    public String link() {
        return link;
    }

    public boolean russianSubtitlesSession() {
        return russianSubtitlesSession;
    }

    public String imageUrl() {
        return this.imageUrl;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Session) obj;
        return Objects.equals(this.dateTime, that.dateTime) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.description, that.description) &&
                Objects.equals(this.verdict, that.verdict) &&
                Objects.equals(this.genres, that.genres) &&
                Objects.equals(this.cinema, that.cinema) &&
                Objects.equals(this.address, that.address) &&
                this.price == that.price &&
                Objects.equals(this.link, that.link) &&
                this.russianSubtitlesSession == that.russianSubtitlesSession &&
                Objects.equals(this.imageUrl, that.imageUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dateTime, name, description, verdict, genres, cinema, address, price, link, russianSubtitlesSession, imageUrl);
    }

    @Override
    public String toString() {
        return "Session[" +
                "dateTime=" + dateTime + ", " +
                "name=" + name + ", " +
                "description=" + description + ", " +
                "verdict=" + verdict + ", " +
                "genres=" + genres + ", " +
                "cinema=" + cinema + ", " +
                "address=" + address + ", " +
                "price=" + price + ", " +
                "link=" + link + ", " +
                "russianSubtitlesSession=" + russianSubtitlesSession + ", " +
                "imageUrl=" + imageUrl + ']';
    }

}
