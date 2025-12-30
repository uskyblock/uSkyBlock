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
            event.setSearchRadius(Settings.island_radius);
            event.setCanCreatePortal(true);
            event.setCreationRadius(Math.max(1, Settings.island_radius - 4));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event) {
        World fromWorld = event.getFrom().getWorld();
        World toWorld = getTargetWorld(fromWorld);

        if (toWorld != null) {
            Location to = getTargetLocation(event.getFrom(), toWorld);
            event.setTo(to);
            event.setSearchRadius(Settings.island_radius);
            event.setCanCreatePortal(false); // Only players should be able to create portals
            event.setCreationRadius(Math.max(1, Settings.island_radius - 4));
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

    private Location getTargetLocation(Location fromLocation, World toWorld) {
        IslandInfo islandInfo = plugin.getIslandInfo(fromLocation);
        if (islandInfo != null && islandInfo.getIslandLocation() != null) {
            Location to = islandInfo.getIslandLocation().clone();
            to.setWorld(toWorld);
            return to;
        }

        // Fallback to 1:1 if no island info found (e.g., in spawn)
        Location to = fromLocation.clone();
        to.setWorld(toWorld);
        return to;
    }
}
