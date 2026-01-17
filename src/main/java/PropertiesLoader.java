package main.java.sift;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Properties loader. */
public final class PropertiesLoader {
    private static final String PROPERTIES_FILE = "app.properties";
    private static final Properties INSTANCE = new Properties();

    static {
        loadProperties();
    }

    private PropertiesLoader() {
    }

    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    private static void loadProperties() {
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                throw new IllegalStateException(PROPERTIES_FILE + " not found in classpath");
            }
            INSTANCE.load(input);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to load " + PROPERTIES_FILE, e);
        }
    }

    public static String get(final String key) {
        return INSTANCE.getProperty(key);
    }
}
