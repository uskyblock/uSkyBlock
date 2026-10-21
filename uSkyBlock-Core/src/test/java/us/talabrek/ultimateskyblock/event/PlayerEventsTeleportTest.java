package us.talabrek.ultimateskyblock.event;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.message.Msg;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.world.WorldManager;

import java.io.File;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The ban and lock checks of {@link PlayerEvents#onTeleport} in the nether island world.
 */
public class PlayerEventsTeleportTest {
    private final World skyWorld = mock(World.class);
    private final World netherWorld = mock(World.class);
    private final Location onTheNetherIsland = new Location(netherWorld, 130, 75, 2);
    private uSkyBlock plugin;
    private PlayerEvents listener;
    private Player player;
    private PlayerInfo playerInfo;

    @BeforeEach
    public void setUp() {
        I18nUtil.initialize(new File("."), Locale.ENGLISH);
        Msg.configure((sender, message) -> {});
        plugin = mock(uSkyBlock.class);
        WorldManager worldManager = mock(WorldManager.class);
        when(plugin.getWorldManager()).thenReturn(worldManager);
        when(worldManager.isSkyWorld(skyWorld)).thenReturn(true);
        when(worldManager.isSkyAssociatedWorld(skyWorld)).thenReturn(true);
        when(worldManager.isSkyAssociatedWorld(netherWorld)).thenReturn(true);
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Visitor");
        playerInfo = mock(PlayerInfo.class);
        when(plugin.getPlayerInfo(player)).thenReturn(playerInfo);
        listener = new PlayerEvents(plugin, mock(RuntimeConfigs.class));
    }

    @AfterEach
    public void tearDown() {
        Msg.configure(null);
    }

    @Test
    public void aBannedPlayerCannotTeleportOntoTheNetherIsland() {
        IslandInfo island = mock(IslandInfo.class);
        when(plugin.getIslandInfo(onTheNetherIsland)).thenReturn(island);
        when(island.isBanned(player.getUniqueId())).thenReturn(true);
        PlayerTeleportEvent event = teleportTo(onTheNetherIsland);

        listener.onTeleport(event);

        assertTrue(event.isCancelled());
    }

    @Test
    public void aVisitorCannotTeleportOntoALockedNetherIsland() {
        IslandInfo island = mock(IslandInfo.class);
        when(plugin.getIslandInfo(onTheNetherIsland)).thenReturn(island);
        when(island.isLocked()).thenReturn(true);
        when(island.getMembers()).thenReturn(Set.of("Owner"));
        PlayerTeleportEvent event = teleportTo(onTheNetherIsland);

        listener.onTeleport(event);

        assertTrue(event.isCancelled());
    }

    @Test
    public void aMemberCanTeleportOntoTheirLockedNetherIsland() {
        IslandInfo island = mock(IslandInfo.class);
        when(plugin.getIslandInfo(onTheNetherIsland)).thenReturn(island);
        when(island.isLocked()).thenReturn(true);
        when(island.getMembers()).thenReturn(Set.of("Owner", "Visitor"));
        PlayerTeleportEvent event = teleportTo(onTheNetherIsland);

        listener.onTeleport(event);

        assertFalse(event.isCancelled());
    }

    @Test
    public void theEntryActionsStayWithTheIslandWorld() {
        listener.onTeleport(teleportTo(onTheNetherIsland));
        verify(playerInfo, never()).onTeleport(player);

        listener.onTeleport(teleportTo(new Location(skyWorld, 130, 150, 2)));
        verify(playerInfo).onTeleport(player);
    }

    private PlayerTeleportEvent teleportTo(Location to) {
        return new PlayerTeleportEvent(player, new Location(mock(World.class), 0, 64, 0), to);
    }
}
