package us.talabrek.ultimateskyblock.event;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
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
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.island.IslandLogic;
import us.talabrek.ultimateskyblock.island.LimitLogic;
import us.talabrek.ultimateskyblock.message.Msg;
import us.talabrek.ultimateskyblock.player.NotificationManager;
import us.talabrek.ultimateskyblock.player.PlayerLogic;
import us.talabrek.ultimateskyblock.player.PlayerNotifier;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.world.WorldManager;

import java.io.File;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class SpawnEventsTest {
    private static final Instant START = Instant.parse("2026-11-13T12:00:00Z");

    private final World world = mock(World.class);
    private final Location location = new Location(world, 0, 100, 0, 90, 0);
    private final WorldManager worldManager = mock(WorldManager.class);
    private final LimitLogic limitLogic = mock(LimitLogic.class);
    private final IslandLogic islandLogic = mock(IslandLogic.class);
    private final IslandInfo islandInfo = mock(IslandInfo.class);
    private final Player member = mock(Player.class, withSettings().extraInterfaces(Audience.class));
    private final Location memberLocation = new Location(world, 5, 100, 5);
    private final Clock clock = mock(Clock.class);
    private SpawnEvents spawnEvents;
    private MockedStatic<WorldGuardHandler> worldGuard;

    @BeforeEach
    public void setUp() throws Exception {
        BukkitServerMock.setupServerMock();
        I18nUtil.initialize(new File("."), Locale.ENGLISH);
        when(world.getUID()).thenReturn(UUID.randomUUID());
        when(worldManager.isSkyAssociatedWorld(world)).thenReturn(true);
        when(islandLogic.getIslandInfo("0,0")).thenReturn(islandInfo);
        when(islandInfo.getMaxHappyGhasts()).thenReturn(4);
        when(islandInfo.getOnlineMembers()).thenReturn(List.of(member));
        when(member.getLocation()).thenReturn(memberLocation);
        when(islandInfo.contains(memberLocation)).thenReturn(true);
        when(clock.instant()).thenReturn(START);
        worldGuard = mockStatic(WorldGuardHandler.class);
        worldGuard.when(() -> WorldGuardHandler.getIslandNameAt(any())).thenReturn("0,0");

        uSkyBlock plugin = mock(uSkyBlock.class);
        PlayerLogic playerLogic = mock(PlayerLogic.class);
        when(plugin.getPlayerLogic()).thenReturn(playerLogic);
        when(playerLogic.getNotificationManager()).thenReturn(new NotificationManager(mock(Plugin.class)));
        spawnEvents = new SpawnEvents(plugin, worldManager, limitLogic, islandLogic, mock(PlayerNotifier.class), clock);
    }

    @AfterEach
    public void tearDown() {
        worldGuard.close();
    }

    @Test
    public void driedGhastFadeCancelledAtLimit() {
        atHappyGhastLimit(true);
        BlockFadeEvent event = fadeEvent(Material.DRIED_GHAST, location);

        spawnEvents.onDriedGhastFade(event);

        assertTrue(event.isCancelled());
        ArgumentCaptor<Component> message = ArgumentCaptor.forClass(Component.class);
        verify(audience()).sendActionBar(message.capture());
        assertEquals("This island has reached its happy ghast limit (4). The dried ghast will hatch once there's room.",
            Msg.plainText(message.getValue()));
    }

    @Test
    public void driedGhastFadeAllowedUnderLimit() {
        atHappyGhastLimit(false);
        BlockFadeEvent event = fadeEvent(Material.DRIED_GHAST, location);

        spawnEvents.onDriedGhastFade(event);

        assertFalse(event.isCancelled());
        verify(audience(), never()).sendActionBar(any(Component.class));
    }

    @Test
    public void otherFadeIgnored() {
        BlockFadeEvent event = fadeEvent(Material.ICE, location);

        spawnEvents.onDriedGhastFade(event);

        assertFalse(event.isCancelled());
        verifyNoInteractions(limitLogic);
    }

    @Test
    public void limitNoticeSkipsMembersOffTheIsland() {
        atHappyGhastLimit(true);
        when(islandInfo.contains(memberLocation)).thenReturn(false);

        spawnEvents.onDriedGhastFade(fadeEvent(Material.DRIED_GHAST, location));

        verify(audience(), never()).sendActionBar(any(Component.class));
    }

    @Test
    public void limitNoticeThrottledPerBlockForAMinute() {
        atHappyGhastLimit(true);

        spawnEvents.onDriedGhastFade(fadeEvent(Material.DRIED_GHAST, location));
        when(clock.instant()).thenReturn(START.plus(Duration.ofSeconds(59)));
        spawnEvents.onDriedGhastFade(fadeEvent(Material.DRIED_GHAST, location));
        verify(audience(), times(1)).sendActionBar(any(Component.class));

        spawnEvents.onDriedGhastFade(fadeEvent(Material.DRIED_GHAST, new Location(world, 3, 100, 0)));
        verify(audience(), times(2)).sendActionBar(any(Component.class));

        when(clock.instant()).thenReturn(START.plus(Duration.ofSeconds(61)));
        spawnEvents.onDriedGhastFade(fadeEvent(Material.DRIED_GHAST, location));
        verify(audience(), times(3)).sendActionBar(any(Component.class));
    }

    @Test
    public void hatchingGhastlingAtLimitCancelledAndDriedGhastRestored() {
        atHappyGhastLimit(true);
        Block block = blockAtSpawn(Material.WATER);
        DriedGhast driedGhast = mock(DriedGhast.class);
        when(driedGhast.getMaximumHydration()).thenReturn(4);
        when(Bukkit.getServer().createBlockData(Material.DRIED_GHAST)).thenReturn(driedGhast);
        CreatureSpawnEvent event = new CreatureSpawnEvent(ghastling(false), CreatureSpawnEvent.SpawnReason.DEFAULT);

        spawnEvents.onCreatureSpawn(event);

        assertTrue(event.isCancelled());
        verify(driedGhast).setWaterlogged(true);
        verify(driedGhast).setHydration(4);
        verify(driedGhast).setFacing(BlockFace.WEST);
        verify(block).setBlockData(driedGhast);
        verify(audience()).sendActionBar(any(Component.class));
    }

    @Test
    public void hatchingGhastlingUnderLimitAllowed() {
        atHappyGhastLimit(false);
        Block block = blockAtSpawn(Material.WATER);
        CreatureSpawnEvent event = new CreatureSpawnEvent(ghastling(false), CreatureSpawnEvent.SpawnReason.DEFAULT);

        spawnEvents.onCreatureSpawn(event);

        assertFalse(event.isCancelled());
        verify(block, never()).setBlockData(any());
    }

    @Test
    public void ghastlingWithOtherReasonAtLimitNotRestored() {
        atHappyGhastLimit(true);
        Block block = blockAtSpawn(Material.AIR);
        CreatureSpawnEvent event = new CreatureSpawnEvent(ghastling(false), CreatureSpawnEvent.SpawnReason.COMMAND);

        spawnEvents.onCreatureSpawn(event);

        assertTrue(event.isCancelled());
        verify(block, never()).setBlockData(any());
        verify(audience(), never()).sendActionBar(any(Component.class));
    }

    @Test
    public void adultHappyGhastAtLimitNotRestored() {
        atHappyGhastLimit(true);
        Block block = blockAtSpawn(Material.AIR);
        CreatureSpawnEvent event = new CreatureSpawnEvent(ghastling(true), CreatureSpawnEvent.SpawnReason.DEFAULT);

        spawnEvents.onCreatureSpawn(event);

        assertTrue(event.isCancelled());
        verify(block, never()).setBlockData(any());
    }

    @Test
    public void ghastlingAtLimitOnOccupiedBlockNotRestored() {
        atHappyGhastLimit(true);
        Block block = blockAtSpawn(Material.STONE);
        CreatureSpawnEvent event = new CreatureSpawnEvent(ghastling(false), CreatureSpawnEvent.SpawnReason.DEFAULT);

        spawnEvents.onCreatureSpawn(event);

        assertTrue(event.isCancelled());
        verify(block, never()).setBlockData(any());
        verify(audience(), never()).sendActionBar(any(Component.class));
    }

    private Audience audience() {
        return (Audience) member;
    }

    private void atHappyGhastLimit(boolean atLimit) {
        when(limitLogic.canSpawn(EntityType.HAPPY_GHAST, islandInfo)).thenReturn(!atLimit);
    }

    private HappyGhast ghastling(boolean adult) {
        HappyGhast ghastling = mock(HappyGhast.class);
        when(ghastling.getType()).thenReturn(EntityType.HAPPY_GHAST);
        when(ghastling.getLocation()).thenReturn(location);
        when(ghastling.isAdult()).thenReturn(adult);
        return ghastling;
    }

    private Block blockAtSpawn(Material type) {
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(type);
        when(world.getBlockAt(location)).thenReturn(block);
        return block;
    }

    private BlockFadeEvent fadeEvent(Material type, Location at) {
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(type);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(at);
        return new BlockFadeEvent(block, mock(BlockState.class));
    }
}
