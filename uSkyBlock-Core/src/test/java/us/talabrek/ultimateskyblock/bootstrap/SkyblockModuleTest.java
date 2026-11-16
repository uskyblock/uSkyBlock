package us.talabrek.ultimateskyblock.bootstrap;

import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.spi.Elements;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import us.talabrek.ultimateskyblock.config.PluginConfig;
import us.talabrek.ultimateskyblock.platform.Platform;
import us.talabrek.ultimateskyblock.platform.Teleports;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.update.ServerPlatform;

import java.nio.file.Path;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SkyblockModuleTest {
    private static final Set<Key<?>> PLATFORM_KEYS =
        Set.of(Key.get(Platform.class), Key.get(ServerPlatform.class), Key.get(Teleports.class));

    @TempDir
    Path dataDir;

    @Test
    void theUpdateCheckAndTheTeleportsComeFromTheDetectedPlatform() {
        Server server = mock(Server.class);
        when(server.getBukkitVersion()).thenReturn("26.3-R0.1-SNAPSHOT");
        Plugin plugin = mock(Plugin.class);
        when(plugin.getServer()).thenReturn(server);

        Injector injector = Guice.createInjector(platformBindings(),
            binder -> binder.bind(Plugin.class).toInstance(plugin));
        Platform platform = injector.getInstance(Platform.class);
        Teleports teleports = injector.getInstance(Teleports.class);

        assertEquals(platform.serverPlatform(server), injector.getInstance(ServerPlatform.class));
        assertSame(platform.teleports().getClass(), teleports.getClass());
        assertSame(teleports, injector.getInstance(Teleports.class));
    }

    /** The module's platform bindings alone: the whole module's graph needs classes the test classpath lacks. */
    private Module platformBindings() {
        uSkyBlock plugin = mock(uSkyBlock.class);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(plugin.getDataFolder()).thenReturn(dataDir.toFile());
        return Elements.getModule(Elements.getElements(new SkyblockModule(plugin, mock(PluginConfig.class))).stream()
            .filter(element -> element instanceof Binding<?> binding && PLATFORM_KEYS.contains(binding.getKey()))
            .toList());
    }
}
