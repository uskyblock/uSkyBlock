package us.talabrek.ultimateskyblock.event;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigFactory;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.world.WorldManager;

import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The ban and lock checks of {@link PlayerEvents#onPlayerRespawn}: a bed or respawn anchor on an island that has
 * banned the player, or been locked to them, since they set it.
 */
public class PlayerEventsRespawnTest {
    private final World skyWorld = mock(World.class);
    private final World netherWorld = mock(World.class);
    private final World otherWorld = mock(World.class);
    private final Location spawn = new Location(skyWorld, 0, 150, 0);
    private final Location anchorOnTheNetherIsland = new Location(netherWorld, 130, 75, 2);
    private final Location bedOnTheIsland = new Location(skyWorld, 130, 150, 2);
    private uSkyBlock plugin;
    private PlayerEvents listener;
    private Player player;
    private IslandInfo island;

    @BeforeEach
    public void setUp() throws Exception {
        plugin = mock(uSkyBlock.class);
        WorldManager worldManager = mock(WorldManager.class);
        when(plugin.getWorldManager()).thenReturn(worldManager);
        when(worldManager.getWorld()).thenReturn(skyWorld);
        when(worldManager.isSkyWorld(skyWorld)).thenReturn(true);
        when(worldManager.isSkyAssociatedWorld(skyWorld)).thenReturn(true);
        when(worldManager.isSkyAssociatedWorld(netherWorld)).thenReturn(true);
        when(skyWorld.getSpawnLocation()).thenReturn(spawn);
        BukkitServerMock.setupServerMock();
        YamlConfiguration config = new YamlConfiguration();
        config.setDefaults(PluginConfigLoader.loadBundledConfig());
        // As bundled: a player with an island respawns on it.
        RuntimeConfig runtimeConfig = new RuntimeConfigFactory(new GameObjectFactory(), Logger.getAnonymousLogger()).load(config);
        RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
        when(runtimeConfigs.current()).thenReturn(runtimeConfig);
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Visitor");
        // Without an island of their own, so they are not sent home.
        when(plugin.getPlayerInfo(player)).thenReturn(mock(PlayerInfo.class));
        island = mock(IslandInfo.class);
        when(island.getMembers()).thenReturn(Set.of("Owner"));
        when(plugin.getIslandInfo(anchorOnTheNetherIsland)).thenReturn(island);
        when(plugin.getIslandInfo(bedOnTheIsland)).thenReturn(island);
        listener = new PlayerEvents(plugin, runtimeConfigs);
    }

    @Test
    public void aBannedPlayerDyingInTheNetherIsSentToSpawnInsteadOfTheirAnchorOnTheIsland() {
        when(island.isBanned(player.getUniqueId())).thenReturn(true);

        assertEquals(spawn, respawn(netherWorld, anchorOnTheNetherIsland));
    }

    @Test
    public void aVisitorDyingOutsideTheIslandWorldsIsSentToSpawnInsteadOfTheirBedOnALockedIsland() {
        when(island.isLocked()).thenReturn(true);

        assertEquals(spawn, respawn(otherWorld, bedOnTheIsland));
    }

    @Test
    public void aTrusteeRespawnsAtTheirAnchorOnALockedIsland() {
        when(island.isLocked()).thenReturn(true);
        when(island.isTrusted(player)).thenReturn(true);

        assertEquals(anchorOnTheNetherIsland, respawn(netherWorld, anchorOnTheNetherIsland));
    }

    @Test
    public void aMemberRespawnsAtTheirBedOnTheirLockedIsland() {
        when(island.isLocked()).thenReturn(true);
        when(island.getMembers()).thenReturn(Set.of("Owner", "Visitor"));

        assertEquals(bedOnTheIsland, respawn(otherWorld, bedOnTheIsland));
    }

    @Test
    public void aBannedPlayerWhoBypassesProtectionRespawnsAtTheirAnchor() {
        when(island.isBanned(player.getUniqueId())).thenReturn(true);
        when(player.hasPermission("usb.mod.bypassprotection")).thenReturn(true);

        assertEquals(anchorOnTheNetherIsland, respawn(netherWorld, anchorOnTheNetherIsland));
    }

    private Location respawn(World diedIn, Location respawnPoint) {
        when(player.getWorld()).thenReturn(diedIn);
        PlayerRespawnEvent event = new PlayerRespawnEvent(player, respawnPoint, false, true,
            PlayerRespawnEvent.RespawnReason.DEATH);
        listener.onPlayerRespawn(event);
        return event.getRespawnLocation();
    }
}
