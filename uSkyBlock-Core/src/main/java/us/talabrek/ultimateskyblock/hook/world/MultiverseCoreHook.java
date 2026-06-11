package us.talabrek.ultimateskyblock.hook.world;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.mvplugins.multiverse.core.MultiverseCore;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.MultiverseWorld;
import org.mvplugins.multiverse.core.world.WorldManager;
import org.mvplugins.multiverse.core.world.options.ImportWorldOptions;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.hook.HookFailedException;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.LocationUtil;

import java.util.logging.Logger;

public class MultiverseCoreHook extends WorldHook {

    private static final String GENERATOR_NAME = "uSkyBlock";

    private final Logger logger;
    private final RuntimeConfigs runtimeConfigs;

    public MultiverseCoreHook(@NotNull uSkyBlock plugin, @NotNull RuntimeConfigs runtimeConfigs) {
        super(plugin, "Multiverse-Core");
        this.logger = plugin.getLogger();
        this.runtimeConfigs = runtimeConfigs;
        setupCore();
    }

    private void setupCore() {
        Plugin mvPlugin = plugin.getServer().getPluginManager().getPlugin("Multiverse-Core");
        if (!(mvPlugin instanceof MultiverseCore)) {
            throw new HookFailedException("Failed to hook into Multiverse-Core");
        }
    }

    @Override
    public void registerOverworld(@NotNull World world) {
        RuntimeConfig runtimeConfig = runtimeConfigs.current();
        WorldManager mvWorldManager = MultiverseCoreApi.get().getWorldManager();

        if (!mvWorldManager.isWorld(world.getName())) {
            logger.info("Importing world: " + world.getName() + " into Multiverse-Core");
            ImportWorldOptions options = ImportWorldOptions
                .worldName(world.getName())
                .environment(World.Environment.NORMAL)
                .useSpawnAdjust(false)
                .generator(GENERATOR_NAME);
            var importResult = mvWorldManager.importWorld(options);
            if (importResult.isFailure()) {
                logger.severe("Failed to import Skyblock overworld into Multiverse-Core: " + importResult.getFailureReason().toString());
                return;
            } else if (importResult.isSuccess()) {
                logger.info("Successfully import world: " + world.getName() + " into Multiverse-Core");
            }
        }

        MultiverseWorld mvWorld = mvWorldManager.getWorld(world).get();
        ensureGeneratorRegistered(mvWorldManager, mvWorld);
        mvWorld.setScale(1.0);

        if (runtimeConfig.general().spawnSize() > 0 && LocationUtil.isEmptyLocation(mvWorld.getSpawnLocation())) {
            Location spawn = LocationUtil.centerOnBlock(
                new Location(world, 0.5, runtimeConfig.island().height() + 0.1, 0.5));
            mvWorld.setAdjustSpawn(false);
            mvWorld.setSpawnLocation(spawn);
            world.setSpawnLocation(spawn);
        }

        if (!runtimeConfig.extras().sendToSpawn()) {
            mvWorld.setRespawnWorld(mvWorld);
        }
    }

    @Override
    public void registerNetherworld(@NotNull World world) {
        RuntimeConfig runtimeConfig = runtimeConfigs.current();
        WorldManager mvWorldManager = MultiverseCoreApi.get().getWorldManager();

        if (!mvWorldManager.isWorld(world.getName())) {
            logger.info("Importing world: " + world.getName() + " into Multiverse-Core");
            ImportWorldOptions options = ImportWorldOptions
                .worldName(world.getName())
                .environment(World.Environment.NETHER)
                .useSpawnAdjust(false)
                .generator(GENERATOR_NAME);
            var importResult = mvWorldManager.importWorld(options);
            if (importResult.isFailure()) {
                logger.severe("Failed to import Skyblock nether world into Multiverse-Core.");
                logger.severe(importResult.getFailureReason().toString());
                return;
            } else if (importResult.isSuccess()) {
                logger.info("Successfully import world: " + world.getName() + " into Multiverse-Core");
            }
        }

        MultiverseWorld mvWorld = mvWorldManager.getWorld(world).get();
        ensureGeneratorRegistered(mvWorldManager, mvWorld);
        mvWorld.setScale(1.0);

        if (runtimeConfig.general().spawnSize() > 0 && LocationUtil.isEmptyLocation(mvWorld.getSpawnLocation())) {
            Location spawn = LocationUtil.centerOnBlock(
                new Location(world, 0.5, runtimeConfig.island().height() / 2.0 + 0.1, 0.5));
            mvWorld.setAdjustSpawn(false);
            mvWorld.setSpawnLocation(spawn);
            world.setSpawnLocation(spawn);
        }

        if (!runtimeConfig.extras().sendToSpawn()) {
            mvWorld.setRespawnWorld(plugin.getWorldManager().getWorld().getName());
        }
    }

    /**
     * Ensures the Multiverse registration for the given world has a generator configured.
     * Multiverse re-applies the stored generator string every time it loads the world; without
     * it, the world loads with the vanilla generator and new chunks get regular terrain.
     * The repair takes effect the next time Multiverse loads the world.
     *
     * @param mvWorldManager Multiverse world manager used to persist the change.
     * @param mvWorld        Multiverse world registration to check.
     */
    void ensureGeneratorRegistered(@NotNull WorldManager mvWorldManager, @NotNull MultiverseWorld mvWorld) {
        String generator = mvWorld.getGenerator();
        if (generator == null || generator.isEmpty()) {
            logger.warning("Multiverse world '" + mvWorld.getName() + "' has no generator configured. "
                + "Setting it to '" + GENERATOR_NAME + "' so it loads as a void world.");
            mvWorld.getStringPropertyHandle().setProperty("generator", GENERATOR_NAME)
                .flatMap(ignored -> mvWorldManager.saveWorldsConfig())
                .onFailure(failure -> logger.severe("Failed to set Multiverse generator for '"
                    + mvWorld.getName() + "': " + failure));
        } else if (!generator.equals(GENERATOR_NAME)) {
            logger.warning("Multiverse world '" + mvWorld.getName() + "' uses generator '" + generator
                + "' instead of '" + GENERATOR_NAME + "'. Leaving it unchanged - make sure it is a void generator.");
        }
    }
}
