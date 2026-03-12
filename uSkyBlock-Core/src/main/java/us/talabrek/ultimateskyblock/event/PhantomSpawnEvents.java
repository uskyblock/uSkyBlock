package us.talabrek.ultimateskyblock.event;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.World;
import org.bukkit.entity.Phantom;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.uSkyBlock;

/**
 * Controls natural phantom spawns on sky worlds.
 */
@Singleton
public class PhantomSpawnEvents implements Listener {
    private final uSkyBlock plugin;

    private boolean phantomsInOverworld;
    private boolean phantomsInNether;

    @Inject
    public PhantomSpawnEvents(@NotNull uSkyBlock plugin) {
        this.plugin = plugin;
        phantomsInOverworld = plugin.getConfig().getBoolean("options.spawning.phantoms.overworld", true);
        phantomsInNether = plugin.getConfig().getBoolean("options.spawning.phantoms.nether", false);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPhantomSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Phantom) ||
            event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) {
            return;
        }

        World spawnWorld = event.getEntity().getWorld();
        if (!phantomsInOverworld && plugin.getWorldManager().isSkyWorld(spawnWorld)) {
            event.setCancelled(true);
        }

        if (!phantomsInNether && plugin.getWorldManager().isSkyNether(spawnWorld)) {
            event.setCancelled(true);
        }
    }

    void setPhantomsInOverworld(boolean state) {
        this.phantomsInOverworld = state;
    }

    void setPhantomsInNether(boolean state) {
        this.phantomsInNether = state;
    }
}
