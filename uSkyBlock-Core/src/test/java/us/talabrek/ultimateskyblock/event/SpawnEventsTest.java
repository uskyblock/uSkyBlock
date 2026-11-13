package us.talabrek.ultimateskyblock.event;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.DriedGhast;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HappyGhast;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.island.IslandLogic;
import us.talabrek.ultimateskyblock.island.LimitLogic;
import us.talabrek.ultimateskyblock.player.PlayerNotifier;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.world.WorldManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class SpawnEventsTest {
    private final World world = mock(World.class);
    private final Location location = new Location(world, 0, 100, 0, 90, 0);
    private final WorldManager worldManager = mock(WorldManager.class);
    private final LimitLogic limitLogic = mock(LimitLogic.class);
    private final IslandLogic islandLogic = mock(IslandLogic.class);
    private final IslandInfo islandInfo = mock(IslandInfo.class);
    private SpawnEvents spawnEvents;
    private MockedStatic<WorldGuardHandler> worldGuard;

    @BeforeEach
    public void setUp() throws Exception {
        BukkitServerMock.setupServerMock();
        when(worldManager.isSkyAssociatedWorld(world)).thenReturn(true);
        when(islandLogic.getIslandInfo("0,0")).thenReturn(islandInfo);
        worldGuard = mockStatic(WorldGuardHandler.class);
        worldGuard.when(() -> WorldGuardHandler.getIslandNameAt(location)).thenReturn("0,0");
        spawnEvents = new SpawnEvents(mock(uSkyBlock.class), worldManager, limitLogic, islandLogic,
            mock(PlayerNotifier.class));
    }

    @AfterEach
    public void tearDown() {
        worldGuard.close();
    }

    @Test
    public void driedGhastFadeCancelledAtLimit() {
        when(limitLogic.canSpawn(EntityType.HAPPY_GHAST, islandInfo)).thenReturn(false);
        BlockFadeEvent event = fadeEvent(Material.DRIED_GHAST);

        spawnEvents.onDriedGhastFade(event);

        assertTrue(event.isCancelled());
    }

    @Test
    public void driedGhastFadeAllowedUnderLimit() {
        when(limitLogic.canSpawn(EntityType.HAPPY_GHAST, islandInfo)).thenReturn(true);
        BlockFadeEvent event = fadeEvent(Material.DRIED_GHAST);

        spawnEvents.onDriedGhastFade(event);

        assertFalse(event.isCancelled());
    }

    @Test
    public void otherFadeIgnored() {
        BlockFadeEvent event = fadeEvent(Material.ICE);

        spawnEvents.onDriedGhastFade(event);

        assertFalse(event.isCancelled());
        verifyNoInteractions(limitLogic);
    }

    @Test
    public void hatchingGhastlingAtLimitCancelledAndDriedGhastRestored() {
        when(limitLogic.canSpawn(EntityType.HAPPY_GHAST, islandInfo)).thenReturn(false);
        Block block = blockAtSpawn(Material.WATER);
        DriedGhast driedGhast = mock(DriedGhast.class);
        when(driedGhast.getMaximumHydration()).thenReturn(4);
        when(Bukkit.getServer().createBlockData(Material.DRIED_GHAST)).thenReturn(driedGhast);
        CreatureSpawnEvent event = new CreatureSpawnEvent(ghastling(), CreatureSpawnEvent.SpawnReason.DEFAULT);

        spawnEvents.onCreatureSpawn(event);

        assertTrue(event.isCancelled());
        verify(driedGhast).setWaterlogged(true);
        verify(driedGhast).setHydration(4);
        verify(driedGhast).setFacing(BlockFace.WEST);
        verify(block).setBlockData(driedGhast);
    }

    @Test
    public void hatchingGhastlingUnderLimitAllowed() {
        when(limitLogic.canSpawn(EntityType.HAPPY_GHAST, islandInfo)).thenReturn(true);
        Block block = blockAtSpawn(Material.WATER);
        CreatureSpawnEvent event = new CreatureSpawnEvent(ghastling(), CreatureSpawnEvent.SpawnReason.DEFAULT);

        spawnEvents.onCreatureSpawn(event);

        assertFalse(event.isCancelled());
        verify(block, never()).setBlockData(any());
    }

    private HappyGhast ghastling() {
        HappyGhast ghastling = mock(HappyGhast.class);
        when(ghastling.getType()).thenReturn(EntityType.HAPPY_GHAST);
        when(ghastling.getLocation()).thenReturn(location);
        when(ghastling.isAdult()).thenReturn(false);
        return ghastling;
    }

    private Block blockAtSpawn(Material type) {
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(type);
        when(world.getBlockAt(location)).thenReturn(block);
        return block;
    }

    private BlockFadeEvent fadeEvent(Material type) {
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(type);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(location);
        return new BlockFadeEvent(block, mock(BlockState.class));
    }
}
