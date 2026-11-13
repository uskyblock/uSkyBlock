package us.talabrek.ultimateskyblock.event;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.DriedGhast;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.api.IslandInfo;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.island.IslandLogic;
import us.talabrek.ultimateskyblock.island.LimitLogic;
import us.talabrek.ultimateskyblock.player.NotificationManager;
import us.talabrek.ultimateskyblock.player.PlayerNotifier;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.world.WorldManager;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static us.talabrek.ultimateskyblock.message.Msg.ERROR;
import static us.talabrek.ultimateskyblock.message.Placeholder.number;

/**
 * Responsible for enforcing island creature spawn limits.
 */
@Singleton
public class SpawnEvents implements Listener {
    private static final Set<Action> RIGHT_CLICKS = Set.of(Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK);
    private static final BlockFace[] FACING_BY_YAW = {BlockFace.SOUTH, BlockFace.WEST, BlockFace.NORTH, BlockFace.EAST};
    private static final Duration HAPPY_GHAST_NOTICE_INTERVAL = Duration.ofSeconds(60);

    private final uSkyBlock plugin;
    private final WorldManager worldManager;
    private final LimitLogic limitLogic;
    private final IslandLogic islandLogic;
    private final PlayerNotifier notifier;
    private final Clock clock;
    private final Map<BlockPosition, Instant> happyGhastNotices = new HashMap<>();

    @Inject
    public SpawnEvents(
        @NotNull uSkyBlock plugin,
        @NotNull WorldManager worldManager,
        @NotNull LimitLogic limitLogic,
        @NotNull IslandLogic islandLogic,
        @NotNull PlayerNotifier notifier,
        @NotNull Clock clock
    ) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        this.limitLogic = limitLogic;
        this.islandLogic = islandLogic;
        this.notifier = notifier;
        this.clock = clock;
    }

    @EventHandler
    public void onSpawnEggEvent(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.useItemInHand() == Event.Result.DENY || !worldManager.isSkyAssociatedWorld(player.getWorld())) {
            return; // Bail out, we don't care
        }
        if (player.hasPermission("usb.mod.bypassprotection") || player.isOp()) {
            return;
        }
        ItemStack item = event.getItem();
        if (RIGHT_CLICKS.contains(event.getAction()) && item != null && item.getItemMeta() instanceof SpawnEggMeta) {
            if (!plugin.playerIsOnIsland(player)) {
                event.setCancelled(true);
                notifier.notifyPlayer(player, tr("You can only use spawn eggs on your own island.", ERROR));
                return;
            }

            if (!canSpawn(getSpawnEggType(item), player.getLocation())) {
                event.setCancelled(true);
                notifier.notifyPlayer(player, tr("You have reached your spawn limit for your island.", ERROR));
                event.setUseItemInHand(Event.Result.DENY);
                event.setUseInteractedBlock(Event.Result.DENY);
            }
        }
    }

    private static @Nullable EntityType getSpawnEggType(@NotNull ItemStack itemStack) {
        if (itemStack.getItemMeta() instanceof SpawnEggMeta spawnEggMeta) {
            EntitySnapshot spawnedEntity = spawnEggMeta.getSpawnedEntity();
            if (spawnedEntity != null) {
                return spawnedEntity.getEntityType();
            } else {
                String key = itemStack.getType().getKey().toString();
                String entityKey = key.replace("_spawn_egg", "");
                NamespacedKey namespacedKey = Objects.requireNonNull(NamespacedKey.fromString(entityKey));
                return Registry.ENTITY_TYPE.get(namespacedKey);
            }
        } else {
            return null;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event == null || !worldManager.isSkyAssociatedWorld(event.getLocation().getWorld())) {
            return; // Bail out, we don't care
        }
        if (event.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.SPAWNER_EGG)) {
            return; // Allow it, the above method would have blocked it if it should be blocked.
        }
        if (!canSpawn(event.getEntity().getType(), event.getLocation())) {
            event.setCancelled(true);
            if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.DEFAULT
                && event.getEntity() instanceof HappyGhast ghastling && !ghastling.isAdult()
                && restoreDriedGhast(event.getLocation())) {
                notifyHappyGhastLimit(event.getLocation());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDriedGhastFade(BlockFadeEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.DRIED_GHAST || !worldManager.isSkyAssociatedWorld(block.getWorld())) {
            return;
        }
        // The dried ghast is removed before its ghastling spawns, so cancelling the spawn would lose the block.
        if (!canSpawn(EntityType.HAPPY_GHAST, block.getLocation())) {
            event.setCancelled(true);
            notifyHappyGhastLimit(block.getLocation());
        }
    }

    /**
     * Spigot fires no fade event for a hatching dried ghast and removes the block before its ghastling spawns, so a
     * ghastling cancelled at the limit puts the block back for vanilla to try again later. In vanilla only a hatching
     * dried ghast spawns a baby happy ghast with the DEFAULT reason (plugins use CUSTOM, commands COMMAND), so this
     * cannot hand out extra dried ghasts.
     */
    private static boolean restoreDriedGhast(@NotNull Location location) {
        Block block = location.getBlock();
        if (block.getType() != Material.WATER && block.getType() != Material.AIR) {
            return false;
        }
        DriedGhast driedGhast = (DriedGhast) Material.DRIED_GHAST.createBlockData();
        driedGhast.setWaterlogged(true);
        driedGhast.setHydration(driedGhast.getMaximumHydration());
        driedGhast.setFacing(FACING_BY_YAW[Math.floorMod(Math.round(location.getYaw() / 90), 4)]);
        block.setBlockData(driedGhast);
        return true;
    }

    private void notifyHappyGhastLimit(@NotNull Location location) {
        IslandInfo islandInfo = activeIslandAt(location);
        if (islandInfo == null) {
            return;
        }
        Instant now = clock.instant();
        happyGhastNotices.values().removeIf(sent -> sent.plus(HAPPY_GHAST_NOTICE_INTERVAL).isBefore(now));
        if (happyGhastNotices.putIfAbsent(BlockPosition.of(location), now) != null) {
            return;
        }
        Component message = tr(
            // I18N: Action bar text when a dried ghast waits because the island is at its happy ghast limit.
            "This island has reached its happy ghast limit (<limit>). The dried ghast will hatch once there's room.",
            ERROR, number("limit", islandInfo.getMaxHappyGhasts()));
        NotificationManager notificationManager = plugin.getPlayerLogic().getNotificationManager();
        for (Player player : islandInfo.getOnlineMembers()) {
            if (islandInfo.contains(player.getLocation())) {
                notificationManager.sendActionBar(player, message);
            }
        }
    }

    private boolean canSpawn(@Nullable EntityType entityType, @NotNull Location location) {
        if (entityType == null) {
            return true; // Only happens on "other-plugins", i.e. EchoPet
        }
        if (entityType.equals(EntityType.GHAST) && location.getWorld().getEnvironment() != World.Environment.NETHER) {
            // Disallow ghasts for now...
            return false;
        }
        IslandInfo islandInfo = activeIslandAt(location);
        return islandInfo != null && limitLogic.canSpawn(entityType, islandInfo);
    }

    private @Nullable IslandInfo activeIslandAt(@NotNull Location location) {
        String islandName = WorldGuardHandler.getIslandNameAt(location);
        return islandName != null ? islandLogic.getIslandInfo(islandName) : null;
    }

    private record BlockPosition(@NotNull UUID world, int x, int y, int z) {
        static BlockPosition of(@NotNull Location location) {
            return new BlockPosition(location.getWorld().getUID(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }
    }
}
