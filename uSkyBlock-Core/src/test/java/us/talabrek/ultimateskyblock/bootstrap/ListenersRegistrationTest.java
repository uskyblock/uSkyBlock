package us.talabrek.ultimateskyblock.bootstrap;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.chat.ChatEvents;
import us.talabrek.ultimateskyblock.command.InviteHandler;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigFactory;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.event.ExploitEvents;
import us.talabrek.ultimateskyblock.event.GriefEvents;
import us.talabrek.ultimateskyblock.event.InternalEvents;
import us.talabrek.ultimateskyblock.event.ItemDropEvents;
import us.talabrek.ultimateskyblock.event.MenuEvents;
import us.talabrek.ultimateskyblock.event.PhantomSpawnEvents;
import us.talabrek.ultimateskyblock.event.PlayerEvents;
import us.talabrek.ultimateskyblock.event.PortalEvents;
import us.talabrek.ultimateskyblock.event.SpawnEvents;
import us.talabrek.ultimateskyblock.event.ToolMenuEvents;
import us.talabrek.ultimateskyblock.event.WitherTagEvents;
import us.talabrek.ultimateskyblock.event.WorldGuardEvents;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.gui.GuiListener;
import us.talabrek.ultimateskyblock.signs.SignEvents;
import us.talabrek.ultimateskyblock.uuid.PlayerDB;

import java.util.Map;
import java.util.logging.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the flag-gated registration logic in {@link Listeners#registerListeners}. Each conditionally
 * registered listener is registered iff its runtime-config flag is on; a handful of listeners register
 * unconditionally.
 *
 * <p>The bundled config.yml enables every gate by default, so "flag on" tests use the default config and
 * "flag off" tests override just the single gate key. The {@code guardianHabitatEvents}
 * ({@code options.spawning.guardians.enabled}) and {@code netherTerraFormEvents} ({@code nether.enabled})
 * gates are intentionally not covered here: those listeners reference WorldGuard types
 * ({@code ProtectedRegion}/{@code ProtectedCuboidRegion}) in their method signatures, so Mockito cannot mock
 * them without WorldGuard on the test classpath. They are passed as {@code null} and left to the live harness.
 */
public class ListenersRegistrationTest {

    private Plugin plugin;
    private PluginManager manager;

    private InternalEvents internalEvents;
    private PlayerEvents playerEvents;
    private PortalEvents portalEvents;
    private GriefEvents griefEvents;
    private ItemDropEvents itemDropEvents;
    private SpawnEvents spawnEvents;
    private WorldGuardEvents worldGuardEvents;
    private ToolMenuEvents toolMenuEvents;

    @BeforeEach
    public void setUp() throws Exception {
        BukkitServerMock.setupServerMock();
    }

    @Test
    public void registersAlwaysOnListenersRegardlessOfFlags() {
        Listeners listeners = buildListeners(loadRuntimeConfig(Map.of()));

        listeners.registerListeners(plugin);

        verify(manager).registerEvents(internalEvents, plugin);
        verify(manager).registerEvents(playerEvents, plugin);
        verify(manager).registerEvents(portalEvents, plugin);
    }

    @Test
    public void registersGriefEventsWhenProtectionEnabled() {
        Listeners listeners = buildListeners(loadRuntimeConfig(Map.of()));

        listeners.registerListeners(plugin);

        verify(manager).registerEvents(griefEvents, plugin);
    }

    @Test
    public void skipsGriefEventsWhenProtectionDisabled() {
        Listeners listeners = buildListeners(loadRuntimeConfig(Map.of("options.protection.enabled", false)));

        listeners.registerListeners(plugin);

        verify(manager, never()).registerEvents(griefEvents, plugin);
    }

    @Test
    public void registersItemDropEventsWhenProtectionAndItemDropsEnabled() {
        Listeners listeners = buildListeners(loadRuntimeConfig(Map.of()));

        listeners.registerListeners(plugin);

        verify(manager).registerEvents(itemDropEvents, plugin);
    }

    @Test
    public void skipsItemDropEventsWhenItemDropsDisabledButKeepsGriefEvents() {
        Listeners listeners = buildListeners(loadRuntimeConfig(Map.of("options.protection.item-drops", false)));

        listeners.registerListeners(plugin);

        // Protection is still enabled, so the outer gate still registers griefEvents...
        verify(manager).registerEvents(griefEvents, plugin);
        // ...but the inner item-drops gate is off.
        verify(manager, never()).registerEvents(itemDropEvents, plugin);
    }

    @Test
    public void registersSpawnEventsWhenSpawnLimitsEnabled() {
        Listeners listeners = buildListeners(loadRuntimeConfig(Map.of()));

        listeners.registerListeners(plugin);

        verify(manager).registerEvents(spawnEvents, plugin);
    }

    @Test
    public void skipsSpawnEventsWhenSpawnLimitsDisabled() {
        Listeners listeners = buildListeners(loadRuntimeConfig(Map.of("options.island.spawn-limits.enabled", false)));

        listeners.registerListeners(plugin);

        verify(manager, never()).registerEvents(spawnEvents, plugin);
    }

    @Test
    public void registersWorldGuardEventsWhenBlockBannedEntryEnabled() {
        Listeners listeners = buildListeners(loadRuntimeConfig(Map.of()));

        listeners.registerListeners(plugin);

        verify(manager).registerEvents(worldGuardEvents, plugin);
    }

    @Test
    public void skipsWorldGuardEventsWhenBlockBannedEntryDisabled() {
        Listeners listeners = buildListeners(loadRuntimeConfig(Map.of("options.protection.visitors.block-banned-entry", false)));

        listeners.registerListeners(plugin);

        verify(manager, never()).registerEvents(worldGuardEvents, plugin);
    }

    @Test
    public void registersToolMenuEventsWhenToolMenuEnabled() {
        Listeners listeners = buildListeners(loadRuntimeConfig(Map.of()));

        listeners.registerListeners(plugin);

        verify(manager).registerEvents(toolMenuEvents, plugin);
    }

    @Test
    public void skipsToolMenuEventsWhenToolMenuDisabled() {
        Listeners listeners = buildListeners(loadRuntimeConfig(Map.of("tool-menu.enabled", false)));

        listeners.registerListeners(plugin);

        verify(manager, never()).registerEvents(toolMenuEvents, plugin);
    }

    private RuntimeConfig loadRuntimeConfig(Map<String, Object> overrides) {
        YamlConfiguration config = new YamlConfiguration();
        config.setDefaults(PluginConfigLoader.loadBundledConfig());
        overrides.forEach(config::set);
        return new RuntimeConfigFactory(new GameObjectFactory(), Logger.getAnonymousLogger()).load(config);
    }

    private Listeners buildListeners(RuntimeConfig runtimeConfig) {
        RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
        when(runtimeConfigs.current()).thenReturn(runtimeConfig);

        plugin = mock(Plugin.class);
        manager = mock(PluginManager.class);
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(manager);

        internalEvents = mock(InternalEvents.class);
        playerEvents = mock(PlayerEvents.class);
        portalEvents = mock(PortalEvents.class);
        griefEvents = mock(GriefEvents.class);
        itemDropEvents = mock(ItemDropEvents.class);
        spawnEvents = mock(SpawnEvents.class);
        worldGuardEvents = mock(WorldGuardEvents.class);
        toolMenuEvents = mock(ToolMenuEvents.class);

        return new Listeners(
            runtimeConfigs,
            mock(GuiListener.class),
            internalEvents,
            playerEvents,
            mock(MenuEvents.class),
            mock(ExploitEvents.class),
            portalEvents,
            mock(WitherTagEvents.class),
            griefEvents,
            itemDropEvents,
            spawnEvents,
            mock(PhantomSpawnEvents.class),
            null, // guardianHabitatEvents: references WorldGuard types in method signatures, not mockable here
            worldGuardEvents,
            null, // netherTerraFormEvents: references WorldGuard types in method signatures, not mockable here
            toolMenuEvents,
            mock(SignEvents.class),
            mock(ChatEvents.class),
            mock(InviteHandler.class),
            mock(PlayerDB.class)
        );
    }
}
