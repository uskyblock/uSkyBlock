package us.talabrek.ultimateskyblock.event;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.Settings;
import us.talabrek.ultimateskyblock.api.IslandInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.world.WorldManager;

@Singleton
public class PortalEvents implements Listener {

    private static final int PORTAL_CREATE_RADIUS = 16;
    private static final int PORTAL_SEARCH_RADIUS = PORTAL_CREATE_RADIUS + 4;

    private final uSkyBlock plugin;
    private final WorldManager worldManager;

    @Inject
    public PortalEvents(@NotNull uSkyBlock plugin, @NotNull WorldManager worldManager) {
        this.plugin = plugin;
        this.worldManager = worldManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            return;
        }

        World fromWorld = event.getFrom().getWorld();
        World toWorld = getTargetWorld(fromWorld);

        if (toWorld != null) {
            Location to = getTargetLocation(event.getFrom(), toWorld);
            event.setTo(to);
            event.setSearchRadius(PORTAL_SEARCH_RADIUS);
            event.setCanCreatePortal(true);
            event.setCreationRadius(PORTAL_CREATE_RADIUS);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event) {
        World fromWorld = event.getFrom().getWorld();
        World toWorld = getTargetWorld(fromWorld);

        if (toWorld != null) {
            Location to = getTargetLocation(event.getFrom(), toWorld);
            event.setTo(to);
            event.setSearchRadius(PORTAL_SEARCH_RADIUS);
            event.setCanCreatePortal(false); // Only players should be able to create portals
            event.setCreationRadius(PORTAL_CREATE_RADIUS);
        }
    }

    private World getTargetWorld(World fromWorld) {
        if (worldManager.isSkyWorld(fromWorld)) {
            return worldManager.getNetherWorld();
        } else if (worldManager.isSkyNether(fromWorld)) {
            return worldManager.getWorld();
        }
        return null;
    }

    // This logic assumes all overworld and nether islands have the same size, which is currently true.
    private Location getTargetLocation(Location fromLocation, World toWorld) {
        // Apply 1:1 scaling
        Location to = fromLocation.clone();
        to.setWorld(toWorld);

        // Nudge the target location into a safe zone (buffer blocks from edge)
        IslandInfo islandInfo = plugin.getIslandInfo(fromLocation);
        if (islandInfo != null) {
            Location center = islandInfo.getIslandLocation();
            if (center != null) {
                int radius = Settings.island_radius;
                int buffer = PORTAL_SEARCH_RADIUS;
                int minX = center.getBlockX() - radius + buffer;
                int maxX = center.getBlockX() + radius - 1 - buffer;
                int minZ = center.getBlockZ() - radius + buffer;
                int maxZ = center.getBlockZ() + radius - 1 - buffer;

                double targetX = Math.max(minX, Math.min(maxX, fromLocation.getX()));
                double targetZ = Math.max(minZ, Math.min(maxZ, fromLocation.getZ()));
                to.setX(targetX);
                to.setZ(targetZ);
            }
        }
        return to;
    }
}
