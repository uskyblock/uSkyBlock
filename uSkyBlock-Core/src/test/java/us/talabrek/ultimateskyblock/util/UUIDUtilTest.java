package us.talabrek.ultimateskyblock.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class UUIDUtilTest {
    private final String TEST_UUID_SHORT = "98c44c3c563a46b6a0e2209cee164958";
    private final String TEST_UUID_LONG = "98c44c3c-563a-46b6-a0e2-209cee164958";

    @Test
    public void fromStringTest() {
        assertNull(UUIDUtil.fromString(null));
        assertNull(UUIDUtil.fromString(""));
        assertNull(UUIDUtil.fromString(TEST_UUID_LONG.substring(30)));

        assertEquals(TEST_UUID_LONG, UUIDUtil.fromString(TEST_UUID_LONG).toString());
        assertEquals(TEST_UUID_LONG, UUIDUtil.fromString(TEST_UUID_SHORT).toString());
    }

    @Test
    public void asStringTest() {
        assertEquals("", UUIDUtil.asString(null));
        assertEquals(TEST_UUID_LONG, UUIDUtil.asString(UUID.fromString(TEST_UUID_LONG)));
    }
}
