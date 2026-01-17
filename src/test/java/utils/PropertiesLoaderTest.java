package utils;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for PropertiesLoader.
 */
public class PropertiesLoaderTest {

    @Test
    public void testGetExistingProperty() {
        String value = PropertiesLoader.get("test.key1");
        assertNotNull(value);
    }
}