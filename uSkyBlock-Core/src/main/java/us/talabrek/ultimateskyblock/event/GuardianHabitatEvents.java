package us.talabrek.ultimateskyblock.event;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.WaterMob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.api.IslandInfo;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.LocationUtil;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Replaces qualified deep-ocean water-mob spawns with guardians using a dedicated cap and chance.
 */
@Singleton
public class GuardianHabitatEvents implements Listener {
    private static final Collection<Material> PRISMARINE_BLOCKS =
        Set.of(Material.PRISMARINE, Material.PRISMARINE_BRICKS, Material.DARK_PRISMARINE);
    private static final Collection<Biome> DEEP_OCEAN_BIOMES =
        Set.of(Biome.DEEP_OCEAN, Biome.DEEP_COLD_OCEAN, Biome.DEEP_FROZEN_OCEAN, Biome.DEEP_LUKEWARM_OCEAN);

    private final uSkyBlock plugin;
    private final GuardianHabitatPolicy policy;

    @Inject
    public GuardianHabitatEvents(@NotNull uSkyBlock plugin) {
        this.plugin = plugin;
        int maxPerIsland = Math.max(0, plugin.getConfig().getInt("options.spawning.guardians.max-per-island", 10));
        double configuredChance = plugin.getConfig().getDouble("options.spawning.guardians.spawn-chance", 0.10d);
        this.policy = new GuardianHabitatPolicy(maxPerIsland, configuredChance);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof WaterMob) || event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
            return;
        }

        Location location = event.getLocation();
        World world = location.getWorld();
        if (world == null || !plugin.getWorldManager().isSkyAssociatedWorld(world)) {
            return;
        }
        if (!isGuardianHabitat(location)) {
            return;
        }
        if (!policy.isEnabled()) {
            return;
        }

        ProtectedRegion islandRegion = WorldGuardHandler.getIslandRegionAt(location);
        if (islandRegion == null) {
            return;
        }
        String islandName = WorldGuardHandler.getIslandNameAt(location);
        IslandInfo islandInfo = islandName != null ? plugin.getIslandInfo(islandName) : null;
        if (islandInfo == null) {
            return;
        }

        event.setCancelled(true);
        boolean generalMonsterLimitAllowsSpawn = !plugin.getConfig().getBoolean("options.island.spawn-limits.enabled", true)
            || plugin.getLimitLogic().canSpawn(EntityType.GUARDIAN, islandInfo);
        if (!policy.shouldSpawnGuardian(countGuardians(world, islandRegion), generalMonsterLimitAllowsSpawn,
            ThreadLocalRandom.current().nextDouble())) {
            return;
        }

        world.spawnEntity(location, EntityType.GUARDIAN);
    }

    private boolean isGuardianHabitat(@NotNull Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        if (!DEEP_OCEAN_BIOMES.contains(world.getBiome(location.getBlockX(), location.getBlockY(), location.getBlockZ()))) {
            return false;
        }
        return PRISMARINE_BLOCKS.contains(LocationUtil.findRoofBlock(location).getType());
    }

    private int countGuardians(@NotNull World world, @NotNull ProtectedRegion region) {
        int count = 0;
        for (LivingEntity entity : WorldGuardHandler.getCreaturesInRegion(world, region)) {
            if (entity.getType() == EntityType.GUARDIAN) {
                count++;
            }
        }
        return count;
    }
}
