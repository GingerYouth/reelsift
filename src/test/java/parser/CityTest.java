package parser;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;

/**
 * Unit tests for City enum.
 */
public class CityTest {

    @Test
    public void testMoscowAsCode() {
        assertEquals("msk", City.MOSCOW.asCode());
    }

    @Test
    public void testSpbAsCode() {
        assertEquals("spb", City.SPB.asCode());
    }

    @Test
    public void testMoscowAsPrefix() {
        assertEquals("MOSCOW:", City.MOSCOW.asPrefix());
    }

    @Test
    public void testSpbAsPrefix() {
        assertEquals("SPB:", City.SPB.asPrefix());
    }
}