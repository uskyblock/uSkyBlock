package us.talabrek.ultimateskyblock.itclient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PresenceClientTest {
    @TempDir Path temporary;

    @Test
    void parsesMinimalConfiguration() {
        PresenceClient.Configuration config = PresenceClient.Configuration.parse(new String[]{
            "--port", "25565", "--name", "UsbItPlayer", "--ready-file", temporary.resolve("ready").toString()
        });
        assertEquals("127.0.0.1", config.host());
        assertEquals(25565, config.port());
        assertEquals("UsbItPlayer", config.name());
    }

    @Test
    void rejectsInvalidOrUnknownOptions() {
        assertThrows(IllegalArgumentException.class, () -> PresenceClient.Configuration.parse(new String[]{"--port", "0"}));
        assertThrows(IllegalArgumentException.class, () -> PresenceClient.Configuration.parse(new String[]{"--wat", "x"}));
    }
}
