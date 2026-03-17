package us.talabrek.ultimateskyblock.world;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.bootstrap.PluginLog;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.handler.AsyncWorldEditHandler;
import us.talabrek.ultimateskyblock.hook.HookManager;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.LocationUtil;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class WorldManager {
    private final Path schematicPath;
    private final HookManager hookManager;
    private final RuntimeConfigs runtimeConfigs;
    private final Scheduler scheduler;
    private final Logger logger;

    public static volatile World skyBlockWorld;
    public static volatile World skyBlockNetherWorld;

    static {
        skyBlockWorld = null;
    }

    @Inject
    public WorldManager(
        @NotNull uSkyBlock plugin,
        @NotNull @PluginLog Logger logger,
        @NotNull RuntimeConfigs runtimeConfigs,
        @NotNull HookManager hookManager,
        @NotNull Scheduler scheduler
    ) {
        this.schematicPath = plugin.getDataFolder().toPath().resolve("schematics").resolve("spawn.schem");
        this.hookManager = hookManager;
        this.runtimeConfigs = runtimeConfigs;
        this.logger = logger;
        this.scheduler = scheduler;
    }

    /**
     * Get the {@link ChunkRegenerator} for the given {@link World}.
     *
     * @param world World to get the ChunkRegenerator for.
     * @return ChunkRegenerator for the given world.
     */
    @NotNull
    public ChunkRegenerator getChunkRegenerator(@NotNull World world) {
        return new ChunkRegenerator(uSkyBlock.getInstance(), runtimeConfigs, world);
    }

    /**
     * Removes all unnamed {@link Monster}'s at the given {@link Location}.
     *
     * @param target Location to remove unnamed monsters.
     */
    public void removeCreatures(@Nullable final Location target) {
        if (!runtimeConfigs.current().island().removeCreaturesByTeleport() || target == null || target.getWorld() == null) {
            return;
        }

        final int px = target.getBlockX();
        final int py = target.getBlockY();
        final int pz = target.getBlockZ();
        for (int x = -1; x <= 1; ++x) {
            for (int z = -1; z <= 1; ++z) {
                Chunk chunk = target.getWorld().getChunkAt(
                    new Location(target.getWorld(), (px + x * 16), py, (pz + z * 16)));

                Arrays.stream(chunk.getEntities())
                    .filter(entity -> entity instanceof Monster)
                    .filter(entity -> entity.getCustomName() == null)
                    .forEach(Entity::remove);
            }
        }
    }

    /**
     * Sets the spawn location for the given {@link World} if currently unset. Creates a safe spawn location if
     * necessary by calling {@link WorldManager#createSpawn(Location)}.
     *
     * @param world        World to setup.
     * @param islandHeight Height at which islands will be created.
     */
    private void setupWorld(@NotNull World world, int islandHeight) {
        Validate.notNull(world, "World cannot be null");

        if (!runtimeConfigs.current().advanced().manageSpawn()) {
            return;
        }

        if (LocationUtil.isEmptyLocation(world.getSpawnLocation())) {
            world.setSpawnLocation(0, islandHeight, 0);
        }

        Location spawnLocation = world.getSpawnLocation();
        if (!LocationUtil.isSafeLocation(spawnLocation)) {
            // Warn the user why we're doing this, because it's a FAQ on the forums:
            logger.warning("Spawn location in " + world.getName() + " is considered unsafe. " +
                "Placing default spawn. This check can be disabled in config.yml, option manageSpawn.");
            createSpawn(spawnLocation);
        }
    }

    /**
     * Creates the world spawn at the given {@link Location}. Places the spawn schematic if
     * configured and when it exists on disk. Places a gold block with two air above it otherwise.
     *
     * @param spawnLocation Location to create the spawn at.
     */
    private void createSpawn(@NotNull Location spawnLocation) {
        Validate.notNull(spawnLocation, "SpawnLocation cannot be null");
        Validate.notNull(spawnLocation.getWorld(), "SpawnLocation#world cannot be null");

        World world = spawnLocation.getWorld();

        if (runtimeConfigs.current().general().spawnSize() > 32 && Files.exists(schematicPath)) {
            AsyncWorldEditHandler.loadIslandSchematic(schematicPath.toFile(), spawnLocation);
        } else {
            Block spawnBlock = world.getBlockAt(spawnLocation).getRelative(BlockFace.DOWN);
            spawnBlock.setType(Material.GOLD_BLOCK);
            Block air = spawnBlock.getRelative(BlockFace.UP);
            air.setType(Material.AIR);
            air.getRelative(BlockFace.UP).setType(Material.AIR);
        }
    }

    /**
     * Gets the {@link ChunkGenerator} responsible for generating chunks in the overworld skyworld.
     *
     * @return ChunkGenerator for overworld skyworld.
     */
    @NotNull
    private ChunkGenerator getOverworldGenerator() {
        try {
            String clazz = runtimeConfigs.current().advanced().chunkGenerator();
            Object generator = Class.forName(clazz).getDeclaredConstructor().newInstance();
            if (generator instanceof ChunkGenerator) {
                return (ChunkGenerator) generator;
            }
        } catch (ClassNotFoundException ex) {
            logger.log(Level.WARNING, "Invalid overworld chunk-generator configured: " + ex);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException |
                 NoSuchMethodException ex) {
            logger.log(Level.WARNING, "Unable to instantiate overworld chunk-generator: " + ex);
        }
        return new SkyBlockChunkGenerator();
    }

    /**
     * Gets the {@link ChunkGenerator} responsible for generating chunks in the nether skyworld.
     *
     * @return ChunkGenerator for nether skyworld.
     */
    @NotNull
    private ChunkGenerator getNetherGenerator() {
        try {
            String clazz = runtimeConfigs.current().nether().chunkGenerator();
            Object generator = Class.forName(clazz).getDeclaredConstructor().newInstance();
            if (generator instanceof ChunkGenerator) {
                return (ChunkGenerator) generator;
            }
        } catch (ClassNotFoundException ex) {
            logger.log(Level.WARNING, "Invalid nether chunk-generator configured: " + ex);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException |
                 NoSuchMethodException ex) {
            logger.log(Level.WARNING, "Unable to instantiate nether chunk-generator: " + ex);
        }
        return new SkyBlockNetherChunkGenerator();
    }

    /**
     * Gets a {@link ChunkGenerator} for use in a default world, as specified in the server configuration
     *
     * @param worldName Name of the world that this will be applied to
     * @param id        Unique ID, if any, that was specified to indicate which generator was requested
     * @return ChunkGenerator for use in the default world generation
     */
    @Nullable
    public ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        Validate.notNull(worldName, "WorldName cannot be null");

        return ((id != null && id.endsWith("nether")) || (worldName.endsWith("nether")))
            && runtimeConfigs.current().nether().enabled()
            ? getNetherGenerator()
            : getOverworldGenerator();
    }

    /**
     * Gets the skyblock island {@link World}. Creates and/or imports the world if necessary.
     *
     * @return Skyblock island world.
     */
    @NotNull
    public synchronized World getWorld() {
        if (skyBlockWorld == null) {
            String worldName = runtimeConfigs.current().general().worldName();
            skyBlockWorld = Bukkit.getWorld(worldName);
            ChunkGenerator skyGenerator = getOverworldGenerator();
            ChunkGenerator worldGenerator = skyBlockWorld != null ? skyBlockWorld.getGenerator() : null;
            if (skyBlockWorld == null
                || skyBlockWorld.canGenerateStructures()
                || worldGenerator == null
                || !worldGenerator.getClass().getName().equals(skyGenerator.getClass().getName())) {
                skyBlockWorld = WorldCreator
                    .name(worldName)
                    .type(WorldType.NORMAL)
                    .generateStructures(false)
                    .environment(World.Environment.NORMAL)
                    .generator(skyGenerator)
                    .createWorld();
                skyBlockWorld.save();
            }

            scheduleOverworldSetup(skyBlockWorld);
        }

        return skyBlockWorld;
    }

    /**
     * Gets the skyblock nether island {@link World}. Creates and/or imports the world if necessary. Returns null if
     * the nether is not enabled in the plugin configuration.
     *
     * @return Skyblock nether island world, or null if nether is disabled.
     */
    @Nullable
    public synchronized World getNetherWorld() {
        RuntimeConfig runtimeConfig = runtimeConfigs.current();
        if (!runtimeConfig.nether().enabled()) {
            return null;
        }

        if (skyBlockNetherWorld == null) {
            String worldName = runtimeConfig.general().worldName();
            skyBlockNetherWorld = Bukkit.getWorld(worldName + "_nether");
            ChunkGenerator skyGenerator = getNetherGenerator();
            ChunkGenerator worldGenerator = skyBlockNetherWorld != null ? skyBlockNetherWorld.getGenerator() : null;
            if (skyBlockNetherWorld == null
                || skyBlockNetherWorld.canGenerateStructures()
                || worldGenerator == null
                || !worldGenerator.getClass().getName().equals(skyGenerator.getClass().getName())) {
                skyBlockNetherWorld = WorldCreator
                    .name(worldName + "_nether")
                    .type(WorldType.NORMAL)
                    .generateStructures(false)
                    .environment(World.Environment.NETHER)
                    .generator(skyGenerator)
                    .createWorld();
                skyBlockNetherWorld.save();
            }

            scheduleNetherSetup(skyBlockNetherWorld);
        }

        return skyBlockNetherWorld;
    }

    /**
     * Checks if the given {@link World} is the skyblock island world.
     *
     * @param world World to check.
     * @return True if the given world is the skyblock island world, false otherwise.
     */
    public boolean isSkyWorld(@Nullable World world) {
        if (world == null) {
            return false;
        }

        return getWorld().getName().equalsIgnoreCase(world.getName());
    }

    /**
     * Checks if the given {@link World} is the skyblock nether island world.
     *
     * @param world World to check.
     * @return True if the given world is the skyblock nether island world, false otherwise.
     */
    public boolean isSkyNether(@Nullable World world) {
        if (world == null) {
            return false;
        }

        World netherWorld = getNetherWorld();
        return netherWorld != null && world.getName().equalsIgnoreCase(netherWorld.getName());
    }

    private void scheduleOverworldSetup(@NotNull World world) {
        scheduler.sync(() -> {
            hookManager.getWorldHook().ifPresent(hook -> hook.registerOverworld(world));
            setupWorld(world, runtimeConfigs.current().island().height());
        });
    }

    private void scheduleNetherSetup(@NotNull World world) {
        scheduler.sync(() -> {
            hookManager.getWorldHook().ifPresent(hook -> hook.registerNetherworld(world));
            hookManager.getInventorySyncHook().ifPresent(hook -> hook.linkNetherInventory(getWorld(), world));
            setupWorld(world, runtimeConfigs.current().island().height() / 2);
        });
    }

    /**
     * Checks if the given {@link World} is associated with Ultimate Skyblock.
     *
     * @param world World to check.
     * @return True if the given world is associated with the plugin, false otherwise.
     */
    public boolean isSkyAssociatedWorld(@Nullable World world) {
        if (world == null) {
            return false;
        }

        return world.getName().startsWith(WorldManager.skyBlockWorld.getName())
            && !(world.getEnvironment() == World.Environment.NETHER && !runtimeConfigs.current().nether().enabled())
            && !(world.getEnvironment() == World.Environment.THE_END);
    }
}
