package us.talabrek.ultimateskyblock.event;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.World;
import org.bukkit.entity.Phantom;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.world.WorldManager;

/**
 * Controls natural phantom spawns on sky worlds.
 */
@Singleton
public class PhantomSpawnEvents implements Listener {
    private final WorldManager worldManager;
    private final RuntimeConfigs runtimeConfigs;

    @Inject
    public PhantomSpawnEvents(@NotNull RuntimeConfigs runtimeConfigs, @NotNull WorldManager worldManager) {
        this.runtimeConfigs = runtimeConfigs;
        this.worldManager = worldManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPhantomSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Phantom) ||
            event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) {
            return;
        }

        World spawnWorld = event.getEntity().getWorld();
        if (!runtimeConfigs.current().spawning().phantoms().overworld() && worldManager.isSkyWorld(spawnWorld)) {
            event.setCancelled(true);
        }

        if (!runtimeConfigs.current().spawning().phantoms().nether() && worldManager.isSkyNether(spawnWorld)) {
            event.setCancelled(true);
        }
    }
}
