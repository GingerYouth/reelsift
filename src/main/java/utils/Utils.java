package utils;

public class Utils {
    /**
     * Removes a date pattern (e.g., /DD-MM-YYYY) and any subsequent parts from a URL.
     * This is used to clean up URLs that may contain dates and other parameters after the main product identifier.
     * For example, "https://ex.com/item/123/01-01-2024#some-tracking-id" -> "https://ex.com/item/123".
     */
    public static String cleanFilmUrl(final String url) {
        return url
            .replaceAll("/\\d{2}-\\d{2}-\\d{4}.*", "")
            .replaceAll("/#.*", "");
    }
}
