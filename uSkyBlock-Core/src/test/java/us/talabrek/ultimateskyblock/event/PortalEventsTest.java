package us.talabrek.ultimateskyblock.event;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigFactory;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.world.WorldManager;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class PortalEventsTest {
    private uSkyBlock plugin;
    private WorldManager worldManager;
    private PortalEvents portalEvents;
    private int radius;

    @BeforeEach
    public void setUp() throws Exception {
        BukkitServerMock.setupServerMock();
        plugin = mock(uSkyBlock.class);
        worldManager = mock(WorldManager.class);

        YamlConfiguration config = new YamlConfiguration();
        config.setDefaults(PluginConfigLoader.loadBundledConfig());
        config.set("options.island.distance", 110);
        config.set("options.island.protectionRange", 100);
        RuntimeConfig runtimeConfig = new RuntimeConfigFactory(new GameObjectFactory(), Logger.getAnonymousLogger()).load(config);
        radius = runtimeConfig.island().radius(); // protectionRange / 2 == 50

        RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
        doReturn(runtimeConfig).when(runtimeConfigs).current();

        portalEvents = new PortalEvents(plugin, worldManager, runtimeConfigs);
    }

    @Test
    public void playerPortalOverworldToNetherRetargetsToIslandLocation() {
        World overworld = mock(World.class);
        World nether = mock(World.class);
        Location from = new Location(overworld, 100, 64, 200);
        Location islandLocation = new Location(overworld, 12, 70, 34);

        PlayerPortalEvent event = mock(PlayerPortalEvent.class);
        doReturn(PlayerTeleportEvent.TeleportCause.NETHER_PORTAL).when(event).getCause();
        doReturn(from).when(event).getFrom();
        doReturn(true).when(worldManager).isSkyWorld(overworld);
        doReturn(nether).when(worldManager).getNetherWorld();

        IslandInfo islandInfo = mock(IslandInfo.class);
        doReturn(islandInfo).when(plugin).getIslandInfo(from);
        doReturn(islandLocation).when(islandInfo).getIslandLocation();

        portalEvents.onPlayerPortal(event);

        ArgumentCaptor<Location> toCaptor = ArgumentCaptor.forClass(Location.class);
        verify(event).setTo(toCaptor.capture());
        Location to = toCaptor.getValue();
        assertEquals(nether, to.getWorld());
        assertEquals(12, to.getBlockX());
        assertEquals(70, to.getBlockY());
        assertEquals(34, to.getBlockZ());

        verify(event).setSearchRadius(radius);
        verify(event).setCanCreatePortal(true);
        verify(event).setCreationRadius(Math.max(1, radius - 4));
    }

    @Test
    public void playerPortalNetherToOverworldFallsBackToFromWhenNoIsland() {
        World nether = mock(World.class);
        World overworld = mock(World.class);
        Location from = new Location(nether, -5, 40, 7);

        PlayerPortalEvent event = mock(PlayerPortalEvent.class);
        doReturn(PlayerTeleportEvent.TeleportCause.NETHER_PORTAL).when(event).getCause();
        doReturn(from).when(event).getFrom();
        doReturn(false).when(worldManager).isSkyWorld(nether);
        doReturn(true).when(worldManager).isSkyNether(nether);
        doReturn(overworld).when(worldManager).getWorld();
        doReturn(null).when(plugin).getIslandInfo(from);

        portalEvents.onPlayerPortal(event);

        ArgumentCaptor<Location> toCaptor = ArgumentCaptor.forClass(Location.class);
        verify(event).setTo(toCaptor.capture());
        Location to = toCaptor.getValue();
        assertEquals(overworld, to.getWorld());
        assertEquals(-5, to.getBlockX());
        assertEquals(40, to.getBlockY());
        assertEquals(7, to.getBlockZ());

        verify(event).setSearchRadius(radius);
        verify(event).setCanCreatePortal(true);
        verify(event).setCreationRadius(Math.max(1, radius - 4));
    }

    @Test
    public void playerPortalIgnoresNonNetherPortalCause() {
        PlayerPortalEvent event = mock(PlayerPortalEvent.class);
        doReturn(PlayerTeleportEvent.TeleportCause.END_PORTAL).when(event).getCause();

        portalEvents.onPlayerPortal(event);

        verify(event, never()).setTo(any());
        verify(event, never()).setSearchRadius(anyInt());
        verify(event, never()).setCanCreatePortal(anyBoolean());
        verify(event, never()).setCreationRadius(anyInt());
    }

    @Test
    public void playerPortalIgnoresWorldOutsideSkyblock() {
        World other = mock(World.class);
        Location from = new Location(other, 0, 64, 0);

        PlayerPortalEvent event = mock(PlayerPortalEvent.class);
        doReturn(PlayerTeleportEvent.TeleportCause.NETHER_PORTAL).when(event).getCause();
        doReturn(from).when(event).getFrom();
        doReturn(false).when(worldManager).isSkyWorld(other);
        doReturn(false).when(worldManager).isSkyNether(other);

        portalEvents.onPlayerPortal(event);

        verify(event, never()).setTo(any());
        verify(event, never()).setSearchRadius(anyInt());
        verify(event, never()).setCanCreatePortal(anyBoolean());
    }

    @Test
    public void entityPortalOverworldToNetherRetargetsButCannotCreatePortal() {
        World overworld = mock(World.class);
        World nether = mock(World.class);
        Location from = new Location(overworld, 8, 60, 9);
        Location islandLocation = new Location(overworld, 1, 65, 2);

        EntityPortalEvent event = mock(EntityPortalEvent.class);
        doReturn(from).when(event).getFrom();
        doReturn(true).when(worldManager).isSkyWorld(overworld);
        doReturn(nether).when(worldManager).getNetherWorld();

        IslandInfo islandInfo = mock(IslandInfo.class);
        doReturn(islandInfo).when(plugin).getIslandInfo(from);
        doReturn(islandLocation).when(islandInfo).getIslandLocation();

        portalEvents.onEntityPortal(event);

        ArgumentCaptor<Location> toCaptor = ArgumentCaptor.forClass(Location.class);
        verify(event).setTo(toCaptor.capture());
        Location to = toCaptor.getValue();
        assertEquals(nether, to.getWorld());
        assertEquals(1, to.getBlockX());
        assertEquals(65, to.getBlockY());
        assertEquals(2, to.getBlockZ());

        verify(event).setSearchRadius(radius);
        verify(event).setCanCreatePortal(false);
        verify(event).setCreationRadius(Math.max(1, radius - 4));
    }
}
