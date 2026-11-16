package us.talabrek.ultimateskyblock.platform;

import org.bukkit.Server;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.update.ServerPlatform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The tests run on spigot-api, as Core compiles, so the Spigot platform is chosen; the Paper one is tested on
 * paper-api, in {@code paperTest}.
 */
class PlatformTest {
    private final Server server = mock(Server.class);

    @Test
    void theSpigotPlatformIsChosenWithSpigotsTeleports() {
        Platform platform = Platform.forServer();

        assertInstanceOf(SpigotPlatform.class, platform);
        assertInstanceOf(SpigotTeleports.class, platform.teleports());
    }

    @Test
    void theUpdateCheckLooksForSpigotPlugins() {
        when(server.getBukkitVersion()).thenReturn("26.3-R0.1-SNAPSHOT");

        assertEquals(ServerPlatform.spigot("26.3"), Platform.forServer().serverPlatform(server));
    }

    @Test
    void theStartLineNamesTheDetectionAndTheServer() {
        when(server.getName()).thenReturn("CraftBukkit");
        when(server.getVersion()).thenReturn("4664-Spigot-bed90fc-e89124d (MC: 26.3)");

        assertEquals("Running on Spigot (CraftBukkit 4664-Spigot-bed90fc-e89124d (MC: 26.3))",
            Platform.forServer().describe(server));
    }
}
