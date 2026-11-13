package us.talabrek.ultimateskyblock.event;

import com.google.inject.Inject;
import com.google.inject.Singleton;
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
import us.talabrek.ultimateskyblock.player.PlayerNotifier;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.world.WorldManager;

import java.util.Objects;
import java.util.Set;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static us.talabrek.ultimateskyblock.message.Msg.ERROR;

/**
 * Responsible for enforcing island creature spawn limits.
 */
@Singleton
public class SpawnEvents implements Listener {
    private static final Set<Action> RIGHT_CLICKS = Set.of(Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK);
    private static final BlockFace[] FACING_BY_YAW = {BlockFace.SOUTH, BlockFace.WEST, BlockFace.NORTH, BlockFace.EAST};

    private final uSkyBlock plugin;
    private final WorldManager worldManager;
    private final LimitLogic limitLogic;
    private final IslandLogic islandLogic;
    private final PlayerNotifier notifier;

    @Inject
    public SpawnEvents(
        @NotNull uSkyBlock plugin,
        @NotNull WorldManager worldManager,
        @NotNull LimitLogic limitLogic,
        @NotNull IslandLogic islandLogic,
        @NotNull PlayerNotifier notifier
    ) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        this.limitLogic = limitLogic;
        this.islandLogic = islandLogic;
        this.notifier = notifier;
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
                && event.getEntity() instanceof HappyGhast ghastling && !ghastling.isAdult()) {
                restoreDriedGhast(event.getLocation());
            }
        }
    }

    /**
     * Spigot fires no fade event for a hatching dried ghast and removes the block before its ghastling spawns, so a
     * ghastling cancelled at the limit puts the block back for vanilla to try again later.
     */
    private static void restoreDriedGhast(@NotNull Location location) {
        Block block = location.getBlock();
        if (block.getType() != Material.WATER) {
            return;
        }
        DriedGhast driedGhast = (DriedGhast) Material.DRIED_GHAST.createBlockData();
        driedGhast.setWaterlogged(true);
        driedGhast.setHydration(driedGhast.getMaximumHydration());
        driedGhast.setFacing(FACING_BY_YAW[Math.floorMod(Math.round(location.getYaw() / 90), 4)]);
        block.setBlockData(driedGhast);
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
        }
    }

    private boolean canSpawn(@Nullable EntityType entityType, @NotNull Location location) {
        if (entityType == null) {
            return true; // Only happens on "other-plugins", i.e. EchoPet
        }
        String islandName = WorldGuardHandler.getIslandNameAt(location);
        if (islandName == null) {
            return false; // Only allow spawning on active islands...
        }
        if (entityType.equals(EntityType.GHAST) && location.getWorld().getEnvironment() != World.Environment.NETHER) {
            // Disallow ghasts for now...
            return false;
        }
        IslandInfo islandInfo = islandLogic.getIslandInfo(islandName);
        if (islandInfo == null) {
            // Disallow spawns on inactive islands
            return false;
        }
        return limitLogic.canSpawn(entityType, islandInfo);
    }
}
