package us.talabrek.ultimateskyblock.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class ConfigDurationTest {

    @Test
    public void parsesSupportedDurationUnits() {
        assertEquals(Duration.ofMillis(250), ConfigDuration.parse("250ms"));
        assertEquals(Duration.ofSeconds(30), ConfigDuration.parse("30s"));
        assertEquals(Duration.ofMinutes(5), ConfigDuration.parse("5m"));
        assertEquals(Duration.ofHours(2), ConfigDuration.parse("2h"));
        assertEquals(Duration.ofDays(3), ConfigDuration.parse("3d"));
    }

    @Test
    public void rejectsBareNumbersAndCompoundValues() {
        assertInvalid("30");
        assertInvalid("PT30S");
        assertInvalid("1m30s");
        assertInvalid("30 S");
    }

    private void assertInvalid(String value) {
        try {
            ConfigDuration.parse(value);
            fail("Expected invalid duration: " + value);
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }
}
