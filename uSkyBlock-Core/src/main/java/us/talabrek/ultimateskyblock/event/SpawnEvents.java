package us.talabrek.ultimateskyblock.event;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.api.IslandInfo;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.uSkyBlock;

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

    private final uSkyBlock plugin;

    @Inject
    public SpawnEvents(@NotNull uSkyBlock plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSpawnEggEvent(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.useItemInHand() == Event.Result.DENY || !plugin.getWorldManager().isSkyAssociatedWorld(player.getWorld())) {
            return; // Bail out, we don't care
        }
        if (player.hasPermission("usb.mod.bypassprotection") || player.isOp()) {
            return;
        }
        ItemStack item = event.getItem();
        if (RIGHT_CLICKS.contains(event.getAction()) && item != null && item.getItemMeta() instanceof SpawnEggMeta) {
            if (!plugin.playerIsOnIsland(player)) {
                event.setCancelled(true);
                plugin.notifyPlayer(player, tr("You can only use spawn eggs on your own island.", ERROR));
                return;
            }

            checkLimits(event, getSpawnEggType(item), player.getLocation());
            if (event.useItemInHand() == Event.Result.DENY) {
                plugin.notifyPlayer(player, tr("You have reached your spawn limit for your island.", ERROR));
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
        if (event == null || !plugin.getWorldManager().isSkyAssociatedWorld(event.getLocation().getWorld())) {
            return; // Bail out, we don't care
        }
        if (event.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.SPAWNER_EGG)) {
            return; // Allow it, the above method would have blocked it if it should be blocked.
        }
        checkLimits(event, event.getEntity().getType(), event.getLocation());
    }

    private void checkLimits(Cancellable event, EntityType entityType, Location location) {
        if (entityType == null) {
            return; // Only happens on "other-plugins", i.e. EchoPet
        }
        String islandName = WorldGuardHandler.getIslandNameAt(location);
        if (islandName == null) {
            event.setCancelled(true); // Only allow spawning on active islands...
            return;
        }
        if (entityType.equals(EntityType.GHAST) && location.getWorld().getEnvironment() != World.Environment.NETHER) {
            // Disallow ghasts for now...
            event.setCancelled(true);
            return;
        }
        IslandInfo islandInfo = plugin.getIslandInfo(islandName);
        if (islandInfo == null) {
            // Disallow spawns on inactive islands
            event.setCancelled(true);
            return;
        }
        if (!plugin.getLimitLogic().canSpawn(entityType, islandInfo)) {
            event.setCancelled(true);
        }
    }
}
