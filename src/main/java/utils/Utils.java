package utils;

public class Utils {
    /**
     * Removes date pattern (DD-MM-YYYY) at the end of the URL.
     */
    public static String removeDateFromUrlEnd(final String url) {
        String cleaned = url.replaceAll("/\\d{2}-\\d{2}-\\d{4}/?$", "");
        if (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned;
    }
}
