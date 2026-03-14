package us.talabrek.ultimateskyblock.bootstrap;

import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.chat.ChatEvents;
import us.talabrek.ultimateskyblock.command.InviteHandler;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.event.ExploitEvents;
import us.talabrek.ultimateskyblock.event.InternalEvents;
import us.talabrek.ultimateskyblock.event.MenuEvents;
import us.talabrek.ultimateskyblock.event.PhantomSpawnEvents;
import us.talabrek.ultimateskyblock.event.PlayerEvents;
import us.talabrek.ultimateskyblock.event.PortalEvents;
import us.talabrek.ultimateskyblock.event.WitherTagEvents;
import us.talabrek.ultimateskyblock.gui.GuiListener;
import us.talabrek.ultimateskyblock.signs.SignEvents;
import us.talabrek.ultimateskyblock.uuid.PlayerDB;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ListenersTest {
    @Test
    public void registersSignEventsWhenSignsAreEnabled() {
        RuntimeConfig runtimeConfig = createRuntimeConfig(true);

        Plugin plugin = mock(Plugin.class);
        PluginManager manager = mock(PluginManager.class);
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(manager);

        SignEvents signEvents = mock(SignEvents.class);
        Listeners listeners = createListeners(runtimeConfig, signEvents);

        listeners.registerListeners(plugin);

        verify(manager).registerEvents(signEvents, plugin);
    }

    @Test
    public void skipsSignEventsWhenSignsAreDisabled() {
        RuntimeConfig runtimeConfig = createRuntimeConfig(false);

        Plugin plugin = mock(Plugin.class);
        PluginManager manager = mock(PluginManager.class);
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(manager);

        SignEvents signEvents = mock(SignEvents.class);
        Listeners listeners = createListeners(runtimeConfig, signEvents);

        listeners.registerListeners(plugin);

        verify(manager, never()).registerEvents(signEvents, plugin);
    }

    private Listeners createListeners(RuntimeConfig runtimeConfig, SignEvents signEvents) {
        RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
        when(runtimeConfigs.current()).thenReturn(runtimeConfig);

        return new Listeners(
            runtimeConfigs,
            mock(GuiListener.class),
            mock(InternalEvents.class),
            mock(PlayerEvents.class),
            mock(MenuEvents.class),
            mock(ExploitEvents.class),
            mock(PortalEvents.class),
            mock(WitherTagEvents.class),
            null,
            null,
            null,
            mock(PhantomSpawnEvents.class),
            null,
            null,
            null,
            null,
            signEvents,
            mock(ChatEvents.class),
            mock(InviteHandler.class),
            mock(PlayerDB.class)
        );
    }

    private RuntimeConfig createRuntimeConfig(boolean signsEnabled) {
        return new RuntimeConfig(
            "en",
            Locale.ENGLISH,
            new RuntimeConfig.Init(Duration.ZERO),
            new RuntimeConfig.General(4, "skyworld", Duration.ZERO, Duration.ZERO, Duration.ZERO, "ocean", "nether_wastes", 64, Duration.ZERO),
            new RuntimeConfig.Island(
                128,
                150,
                false,
                128,
                64,
                List.of(),
                true,
                Map.of(),
                true,
                true,
                true,
                "default",
                Duration.ZERO,
                Duration.ZERO,
                false,
                Duration.ZERO,
                0.5d,
                Duration.ZERO,
                true,
                10,
                true,
                "",
                new RuntimeConfig.SpawnLimits(false, 0, 0, 0, 0, 0),
                Map.of()
            ),
            new RuntimeConfig.Extras(false, true, true),
            new RuntimeConfig.Protection(false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false),
            new RuntimeConfig.Nether(false, 7, 75, "", new RuntimeConfig.Terraform(false, 0d, 0d, 0, Map.of(), Map.of()), new RuntimeConfig.SpawnChances(false, 0d, 0d, 0d)),
            new RuntimeConfig.Restart(true, true, true, true, false, true, Duration.ZERO, List.of()),
            new RuntimeConfig.Advanced(Duration.ZERO, false, 0d, true, "", "", "", "", Duration.ZERO, Duration.ZERO, "", 4, Duration.ZERO, 0d, Duration.ZERO, null,
                new RuntimeConfig.PlayerDb("bukkit", "", "", Duration.ZERO)),
            new RuntimeConfig.Async(Duration.ZERO, 0L, Duration.ZERO),
            new RuntimeConfig.AsyncWorldEdit(false, Duration.ZERO, Duration.ZERO),
            new RuntimeConfig.Party(Duration.ZERO, "", List.of(), List.of(), Map.of()),
            new RuntimeConfig.PluginUpdates(true, "RELEASE"),
            new RuntimeConfig.Spawning(new RuntimeConfig.Guardians(false, 0, 0d), new RuntimeConfig.Phantoms(true, false)),
            new RuntimeConfig.Placeholder(false, false, false),
            new RuntimeConfig.ToolMenu(false, null, List.of()),
            new RuntimeConfig.Signs(signsEnabled),
            new RuntimeConfig.WorldGuard(true, true),
            new RuntimeConfig.Importer(0.1d, Duration.ZERO),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of()
        );
    }
}
