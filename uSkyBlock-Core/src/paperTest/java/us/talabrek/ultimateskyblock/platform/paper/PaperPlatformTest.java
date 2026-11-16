package us.talabrek.ultimateskyblock.platform.paper;

import org.bukkit.Server;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.platform.Platform;
import us.talabrek.ultimateskyblock.update.ServerPlatform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** On paper-api, as on Paper and its forks: the Paper platform is chosen. */
class PaperPlatformTest {
    private final Server server = mock(Server.class);

    @Test
    void thePaperPlatformIsChosenWithPapersTeleports() {
        Platform platform = Platform.forServer();

        assertInstanceOf(PaperPlatform.class, platform);
        assertInstanceOf(PaperTeleports.class, platform.teleports());
    }

    @Test
    void theUpdateCheckLooksForPaperPlugins() {
        when(server.getBukkitVersion()).thenReturn("1.21.10-R0.1-SNAPSHOT");

        assertEquals(ServerPlatform.paper("1.21.10"), Platform.forServer().serverPlatform(server));
    }

    @Test
    void theStartLineNamesTheDetectionAndTheServerAForkOfPaperToo() {
        when(server.getName()).thenReturn("Purpur");
        when(server.getVersion()).thenReturn("26.3-2658-abc1234 (MC: 26.3)");

        assertEquals("Running on Paper (Purpur 26.3-2658-abc1234 (MC: 26.3))", Platform.forServer().describe(server));
    }
}
