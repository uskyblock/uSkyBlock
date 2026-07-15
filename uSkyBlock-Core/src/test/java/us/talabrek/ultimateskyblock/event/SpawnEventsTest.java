package us.talabrek.ultimateskyblock.event;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.island.IslandLogic;
import us.talabrek.ultimateskyblock.island.LimitLogic;
import us.talabrek.ultimateskyblock.player.PlayerNotifier;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.world.WorldManager;

import java.io.File;
import java.util.Locale;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SpawnEvents} covering only the branches reachable without WorldGuard.
 * The {@code checkLimits(...)} spawn-limit path relies on {@code WorldGuardHandler.getIslandNameAt(...)}
 * (WorldGuard is compileOnly and off the test classpath), so those branches are left to the live-server harness.
 */
public class SpawnEventsTest {
    private uSkyBlock plugin;
    private WorldManager worldManager;
    private PlayerNotifier notifier;
    private SpawnEvents spawnEvents;

    @BeforeEach
    public void setUp() throws Exception {
        BukkitServerMock.setupServerMock();
        I18nUtil.initialize(new File("."), Locale.ENGLISH);

        plugin = mock(uSkyBlock.class);
        worldManager = mock(WorldManager.class);
        notifier = mock(PlayerNotifier.class);
        spawnEvents = new SpawnEvents(plugin, worldManager, mock(LimitLogic.class), mock(IslandLogic.class), notifier);
    }

    @Test
    public void spawnEggDeniedWhenVisitorOffIsland() {
        Player player = mock(Player.class);
        when(player.getWorld()).thenReturn(mock(World.class));
        when(worldManager.isSkyAssociatedWorld(any())).thenReturn(true);
        when(plugin.playerIsOnIsland(player)).thenReturn(false);

        ItemStack item = mock(ItemStack.class);
        when(item.getItemMeta()).thenReturn(mock(SpawnEggMeta.class));

        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.useItemInHand()).thenReturn(Event.Result.ALLOW);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
        when(event.getItem()).thenReturn(item);

        spawnEvents.onSpawnEggEvent(event);

        verify(event).setCancelled(true);
        verify(notifier).notifyPlayer(eq(player), any());
    }

    @Test
    public void creatureSpawnAllowedForSpawnerEgg() {
        World world = mock(World.class);
        when(worldManager.isSkyAssociatedWorld(world)).thenReturn(true);

        CreatureSpawnEvent event = mock(CreatureSpawnEvent.class);
        when(event.getLocation()).thenReturn(new Location(world, 0, 0, 0));
        when(event.getSpawnReason()).thenReturn(CreatureSpawnEvent.SpawnReason.SPAWNER_EGG);

        spawnEvents.onCreatureSpawn(event);

        verify(event, never()).setCancelled(anyBoolean());
    }

    @Test
    public void creatureSpawnIgnoredOutsideSkyworld() {
        World world = mock(World.class);
        when(worldManager.isSkyAssociatedWorld(any())).thenReturn(false);

        CreatureSpawnEvent event = mock(CreatureSpawnEvent.class);
        when(event.getLocation()).thenReturn(new Location(world, 0, 0, 0));

        spawnEvents.onCreatureSpawn(event);

        verify(event, never()).setCancelled(anyBoolean());
    }
}
