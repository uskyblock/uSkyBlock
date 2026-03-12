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
import us.talabrek.ultimateskyblock.config.PluginConfig;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.island.IslandLogic;
import us.talabrek.ultimateskyblock.island.LimitLogic;
import us.talabrek.ultimateskyblock.util.LocationUtil;
import us.talabrek.ultimateskyblock.world.WorldManager;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Applies guardian-only spawn rules to qualified deep-ocean prismarine habitats.
 * Eligible water-mob spawns are suppressed, and guardians are spawned instead only when the
 * configured cap and chance allow it. This preserves the existing behavior of not spawning
 * other water mobs in these habitats.
 */
@Singleton
public class GuardianHabitatEvents implements Listener {
    private static final Collection<Material> PRISMARINE_BLOCKS =
        Set.of(Material.PRISMARINE, Material.PRISMARINE_BRICKS, Material.DARK_PRISMARINE);
    private static final Collection<Biome> DEEP_OCEAN_BIOMES =
        Set.of(Biome.DEEP_OCEAN, Biome.DEEP_COLD_OCEAN, Biome.DEEP_FROZEN_OCEAN, Biome.DEEP_LUKEWARM_OCEAN);

    private final PluginConfig config;
    private final WorldManager worldManager;
    private final LimitLogic limitLogic;
    private final IslandLogic islandLogic;
    private final GuardianHabitatPolicy policy;

    @Inject
    public GuardianHabitatEvents(
        @NotNull PluginConfig config,
        @NotNull WorldManager worldManager,
        @NotNull LimitLogic limitLogic,
        @NotNull IslandLogic islandLogic
    ) {
        this.config = config;
        this.worldManager = worldManager;
        this.limitLogic = limitLogic;
        this.islandLogic = islandLogic;
        int maxPerIsland = Math.max(0, config.getYamlConfig().getInt("options.spawning.guardians.max-per-island", 10));
        double configuredChance = config.getYamlConfig().getDouble("options.spawning.guardians.spawn-chance", 0.10d);
        this.policy = new GuardianHabitatPolicy(maxPerIsland, configuredChance);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof WaterMob) || event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
            return;
        }

        Location location = event.getLocation();
        World world = location.getWorld();
        if (world == null || !worldManager.isSkyAssociatedWorld(world)) {
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
        IslandInfo islandInfo = islandName != null ? islandLogic.getIslandInfo(islandName) : null;
        if (islandInfo == null) {
            return;
        }

        event.setCancelled(true);
        boolean generalMonsterLimitAllowsSpawn = !config.getYamlConfig().getBoolean("options.island.spawn-limits.enabled", true)
            || limitLogic.canSpawn(EntityType.GUARDIAN, islandInfo);
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
