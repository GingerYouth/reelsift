package main.java.sift;

import java.util.List;

/** Movie DTO. */
public record Movie(String title, List<String> genres, String description, String verdict) {
    public String llmInputFormat() {
        return String.format(
            """
            Title: %s
            Genres: %s
            Description: %s
            Verdict: %s
            ---
            """,
            title(),
            genresString(),
            description(),
            verdict()
        );
    }

    private String genresString() {
        return genres().isEmpty() ? "N/A" : String.join(", ", genres());
    }
}