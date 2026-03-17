package us.talabrek.ultimateskyblock.island;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IslandKeyTest {

    @Test
    public void acceptsLocationDerivedIslandKeys() {
        assertEquals("128,-256", IslandKey.fromIslandName("128,-256").value());
    }

    @Test
    public void rejectsBlankKeys() {
        assertThrows(IllegalArgumentException.class, () -> new IslandKey(" "));
    }

    @Test
    public void rejectsUnexpectedKeyFormats() {
        assertThrows(IllegalArgumentException.class, () -> new IslandKey("skyworld:128,-256"));
    }
}
