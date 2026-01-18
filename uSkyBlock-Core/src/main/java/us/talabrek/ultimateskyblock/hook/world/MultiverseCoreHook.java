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
import us.talabrek.ultimateskyblock.Settings;
import us.talabrek.ultimateskyblock.hook.HookFailedException;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.LocationUtil;

import java.util.logging.Logger;

public class MultiverseCoreHook extends WorldHook {

    private static final String GENERATOR_NAME = "uSkyBlock";

    private final Logger logger;

    public MultiverseCoreHook(@NotNull uSkyBlock plugin) {
        super(plugin, "Multiverse-Core");
        this.logger = plugin.getLogger();
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
        mvWorld.setScale(1.0);

        if (Settings.general_spawnSize > 0 && LocationUtil.isEmptyLocation(mvWorld.getSpawnLocation())) {
            Location spawn = LocationUtil.centerOnBlock(
                new Location(world, 0.5, Settings.island_height + 0.1, 0.5));
            mvWorld.setAdjustSpawn(false);
            mvWorld.setSpawnLocation(spawn);
            world.setSpawnLocation(spawn);
        }

        if (!Settings.extras_sendToSpawn) {
            mvWorld.setRespawnWorld(mvWorld);
        }
    }

    @Override
    public void registerNetherworld(@NotNull World world) {
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
        mvWorld.setScale(1.0);

        if (Settings.general_spawnSize > 0 && LocationUtil.isEmptyLocation(mvWorld.getSpawnLocation())) {
            Location spawn = LocationUtil.centerOnBlock(
                new Location(world, 0.5, Settings.island_height / 2.0 + 0.1, 0.5));
            mvWorld.setAdjustSpawn(false);
            mvWorld.setSpawnLocation(spawn);
            world.setSpawnLocation(spawn);
        }

        if (!Settings.extras_sendToSpawn) {
            mvWorld.setRespawnWorld(plugin.getWorldManager().getWorld().getName());
        }
    }
}
