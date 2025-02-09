package us.talabrek.ultimateskyblock.island.level;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dk.lockfuglsang.minecraft.file.FileUtil;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.PluginConfig;
import us.talabrek.ultimateskyblock.api.async.Callback;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.island.task.ChunkSnapShotTask;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.Scheduler;
import us.talabrek.ultimateskyblock.world.WorldManager;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Business logic regarding the calculation of level
 */
@Singleton
public class ChunkSnapshotLevelLogic extends CommonLevelLogic {

    private final Plugin plugin;
    private final PluginConfig pluginConfig;
    private final Scheduler scheduler;
    private final Logger logger;

    @Inject
    public ChunkSnapshotLevelLogic(
        @NotNull uSkyBlock plugin,
        @NotNull WorldManager worldManager,
        @NotNull PluginConfig pluginConfig,
        @NotNull Scheduler scheduler,
        @NotNull Logger logger
    ) {
        super(FileUtil.getYmlConfiguration("levelConfig.yml"), worldManager);
        this.plugin = plugin;
        this.pluginConfig = pluginConfig;
        this.scheduler = scheduler;
        this.logger = logger;
    }

    @Override
    public void calculateScoreAsync(final Location l, final Callback<IslandScore> callback) {
        // TODO: 10/05/2015 - R4zorax: Ensure no overlapping calls to this one happen...
        logger.entering(this.getClass().getName(), "calculateScoreAsync");
        // is further threading needed here?
        final ProtectedRegion region = WorldGuardHandler.getIslandRegionAt(l);
        if (region == null) {
            return;
        }
        new ChunkSnapShotTask(scheduler, pluginConfig, l, region, new Callback<>() {
            @Override
            public void run() {
                final List<ChunkSnapshot> snapshotsOverworld = getState();
                Location netherLoc = getNetherLocation(l);
                final ProtectedRegion netherRegion = WorldGuardHandler.getNetherRegionAt(netherLoc);
                new ChunkSnapShotTask(scheduler, pluginConfig, netherLoc, netherRegion, new Callback<>() {
                    @Override
                    public void run() {
                        final List<ChunkSnapshot> snapshotsNether = getState();
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                calculateScoreAndCallback(region, snapshotsOverworld, netherRegion, snapshotsNether, callback);
                            }
                        }.runTaskAsynchronously(plugin);
                    }
                }).runTask(plugin);
            }
        }).runTask(plugin);
    }

    private void calculateScoreAndCallback(ProtectedRegion region, List<ChunkSnapshot> snapshotsOverworld, ProtectedRegion netherRegion, List<ChunkSnapshot> snapshotsNether, Callback<IslandScore> callback) {
        IslandScore islandScore = calculateScore(region, snapshotsOverworld, netherRegion, snapshotsNether);
        callback.setState(islandScore);
        scheduler.sync(callback);
        logger.exiting(this.getClass().getName(), "calculateScoreAsync");
    }

    private IslandScore calculateScore(ProtectedRegion region, List<ChunkSnapshot> snapshotsOverworld, ProtectedRegion netherRegion, List<ChunkSnapshot> snapshotsNether) {
        final BlockCountCollection counts = new BlockCountCollection(scoreMap);
        int minX = region.getMinimumPoint().getBlockX();
        int maxX = region.getMaximumPoint().getBlockX();
        int minY = region.getMinimumPoint().getBlockY();
        int maxY = region.getMaximumPoint().getBlockY();
        int minZ = region.getMinimumPoint().getBlockZ();
        int maxZ = region.getMaximumPoint().getBlockZ();

        for (int x = minX; x <= maxX; ++x) {
            for (int z = minZ; z <= maxZ; ++z) {
                ChunkSnapshot chunk = getChunkSnapshot(x >> 4, z >> 4, snapshotsOverworld);
                if (chunk == null) {
                    // This should NOT happen!
                    logger.log(Level.WARNING, "Missing chunk in snapshot for x,z = " + x + "," + z);
                    continue;
                }
                int cx = (x & 0xf);
                int cz = (z & 0xf);
                for (int y = minY; y < maxY; y++) {
                    Material blockType = chunk.getBlockType(cx, y, cz);
                    if (blockType == Material.AIR) {
                        continue;
                    }
                    counts.add(blockType);
                }
            }
        }
        IslandScore islandScore = createIslandScore(counts);
        if (islandScore.getScore() >= activateNetherAtLevel && netherRegion != null && snapshotsNether != null) {
            // Add nether levels
            minX = netherRegion.getMinimumPoint().getBlockX();
            maxX = netherRegion.getMaximumPoint().getBlockX();
            minZ = netherRegion.getMinimumPoint().getBlockZ();
            maxZ = netherRegion.getMaximumPoint().getBlockZ();
            for (int x = minX; x <= maxX; ++x) {
                for (int z = minZ; z <= maxZ; ++z) {
                    ChunkSnapshot chunk = getChunkSnapshot(x >> 4, z >> 4, snapshotsNether);
                    if (chunk == null) {
                        // This should NOT happen!
                        logger.log(Level.WARNING, "Missing nether-chunk in snapshot for x,z = " + x + "," + z);
                        continue;
                    }
                    int cx = (x & 0xf);
                    int cz = (z & 0xf);
                    for (int y = 6; y < 120; y++) {
                        Material blockType = chunk.getBlockType(cx, y, cz);
                        if (blockType == Material.AIR) {
                            continue;
                        }
                        counts.add(blockType);
                    }
                }
            }
            islandScore = createIslandScore(counts);
        }
        return islandScore;
    }

    private static ChunkSnapshot getChunkSnapshot(int x, int z, List<ChunkSnapshot> snapshots) {
        for (ChunkSnapshot chunk : snapshots) {
            if (chunk.getX() == x && chunk.getZ() == z) {
                return chunk;
            }
        }
        return null;
    }
}
