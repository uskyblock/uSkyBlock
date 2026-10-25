package us.talabrek.ultimateskyblock.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.bootstrap.PluginDataDir;
import us.talabrek.ultimateskyblock.bootstrap.PluginLog;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.island.IslandLogic;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.IslandUtil;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Brings the locks of a server's nether island regions in line with their islands, and removes the nether regions of
 * deleted islands, once. Before 3.7.0, {@code /is lock} and {@code /is unlock} changed only an island's region in the
 * island world, so its nether region kept the lock it had when it was last rebuilt; and deleting an island left its
 * nether region behind, still locked and owned by its last party. The first start gives every island's nether region
 * the island's lock, removes the nether regions of islands that no longer exist, and leaves a marker so it never runs
 * again. It works through a few islands each tick, so a server with thousands of them does not stall.
 * <p>
 * It leaves the regions in the island world, and the owners and members of the nether regions, as they are. They may
 * differ from the island's for good reasons: a lock set with WorldGuard's own commands, owners and members added with
 * them, or real UUIDs where the island file holds the offline-mode UUIDs of an old name-to-UUID conversion. The repair
 * in 3.7.0 replaced them with the island's, and players lost access to their islands. It left the same marker, so the
 * repair does not run again where that one ran.
 * <p>
 * It leaves saving the regions to WorldGuard, which saves changed regions every 30 seconds on a thread of its own,
 * under a lock no other plugin can take: a save of the repair's own could overlap one. The marker follows two minutes
 * later instead, so a crash before WorldGuard's next save does not leave the marker of a repair that was lost.
 */
@Singleton
public class IslandRegionRepair {
    /** Under the plugin's {@code data/} folder: there once the repair has run. */
    static final String MARKER_FILE = "island-region-repair.done";
    /** How many islands, or nether regions, the repair works through in one tick. */
    static final int PER_TICK = 20;
    /** How long after the repair the marker follows: well after WorldGuard's next save. */
    static final Duration MARKER_DELAY = Duration.ofMinutes(2);
    private static final Pattern NETHER_REGION_ID = Pattern.compile("(-?\\d+,-?\\d+)nether");

    private final Path pluginDataDir;
    private final Logger logger;
    private final uSkyBlock plugin;
    private final IslandLogic islandLogic;
    private final RuntimeConfigs runtimeConfigs;
    private final Scheduler scheduler;
    private final IslandRegions regions;
    private Run run;

    @Inject
    public IslandRegionRepair(@NotNull @PluginDataDir Path pluginDataDir, @NotNull @PluginLog Logger logger,
                              @NotNull uSkyBlock plugin, @NotNull IslandLogic islandLogic,
                              @NotNull RuntimeConfigs runtimeConfigs, @NotNull Scheduler scheduler) {
        this(pluginDataDir, logger, plugin, islandLogic, runtimeConfigs, scheduler, new WorldGuardRegions(plugin));
    }

    IslandRegionRepair(@NotNull Path pluginDataDir, @NotNull Logger logger, @NotNull uSkyBlock plugin,
                       @NotNull IslandLogic islandLogic, @NotNull RuntimeConfigs runtimeConfigs,
                       @NotNull Scheduler scheduler, @NotNull IslandRegions regions) {
        this.pluginDataDir = pluginDataDir;
        this.logger = logger;
        this.plugin = plugin;
        this.islandLogic = islandLogic;
        this.runtimeConfigs = runtimeConfigs;
        this.scheduler = scheduler;
        this.regions = regions;
    }

    /**
     * Starts the repair, unless it has run already. Without WorldGuard, or when the islands cannot be listed, it
     * waits for the next start.
     */
    public void start() {
        if (Files.exists(markerFile())) {
            return;
        }
        if (!regions.available()) {
            logger.info("WorldGuard is not enabled, so the nether regions are synced to their islands at a later"
                + " start.");
            return;
        }
        List<String> islands;
        try {
            islands = listIslands();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to list the islands, so their regions are synced at the next start", e);
            return;
        }
        // The islands with a nether region, by its id.
        List<String> netherIslands = regions.netherRegionIds().stream()
            .map(NETHER_REGION_ID::matcher)
            .filter(Matcher::matches)
            .map(matcher -> matcher.group(1))
            .sorted()
            .toList();
        logger.info("Syncing the nether regions of " + islands.size() + " islands to their lock, and removing those"
            + " of deleted islands. The regions in the island world, and owners and members, stay as they are. This"
            + " runs once, " + PER_TICK + " islands a tick.");
        run = new Run(islands.iterator(), netherIslands.iterator());
        run.task = scheduler.sync(run, Duration.ZERO, Duration.ofMillis(50));
    }

    /**
     * Stops a repair still running, or the marker still to follow it, at a reload or a stop: the repair uses the
     * services of this start, and the next start runs it again from the beginning, as it has left no marker. That is
     * harmless: it sets the same locks again, and the nether regions it removed are gone.
     */
    public void shutdown() {
        if (run != null) {
            run.task.cancel();
        }
    }

    private List<String> listIslands() throws IOException {
        Path directory = islandDirectory();
        FilenameFilter islandFiles = IslandUtil.createIslandFilenameFilter(runtimeConfigs.current().general().spawnSize());
        List<String> islands = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory)) {
            for (Path file : files) {
                String fileName = file.getFileName().toString();
                if (islandFiles.accept(directory.toFile(), fileName)) {
                    islands.add(fileName.substring(0, fileName.length() - ".yml".length()));
                }
            }
        } catch (DirectoryIteratorException e) {
            // How the stream reports an I/O error while it is iterated.
            throw e.getCause();
        }
        islands.sort(Comparator.naturalOrder());
        return islands;
    }

    private Path islandDirectory() {
        return pluginDataDir.resolve("islands");
    }

    private Path islandFile(String islandName) {
        return islandDirectory().resolve(islandName + ".yml");
    }

    private Path markerFile() {
        return pluginDataDir.resolve("data").resolve(MARKER_FILE);
    }

    private final class Run implements Runnable {
        private final Iterator<String> islands;
        private final Iterator<String> netherIslands;
        /** The repair's ticks, then the marker that follows them: what a reload or a stop cancels. */
        private BukkitTask task;
        private int synced;
        private int unreadable;
        private int removed;

        private Run(Iterator<String> islands, Iterator<String> netherIslands) {
            this.islands = islands;
            this.netherIslands = netherIslands;
        }

        @Override
        public void run() {
            if (plugin.isMaintenanceMode()) {
                // The islands cannot be read in maintenance mode, so the repair waits for it to end.
                return;
            }
            for (int i = 0; i < PER_TICK; i++) {
                if (islands.hasNext()) {
                    sync(islands.next());
                } else if (netherIslands.hasNext()) {
                    removeIfOrphaned(netherIslands.next());
                } else {
                    task.cancel();
                    finish();
                    return;
                }
            }
        }

        private void sync(String islandName) {
            // As generateTopTen does: an island loaded only for the repair leaves memory again, so it does not push
            // out the islands of the players on the server, and changes made to those are not lost.
            boolean wasInMemory = islandLogic.isIslandInMemory(islandName);
            if (!wasInMemory && Files.notExists(islandFile(islandName))) {
                // Deleted since the repair listed the islands, and its regions with it.
                return;
            }
            try {
                IslandInfo island = islandLogic.getIslandInfo(islandName);
                if (island.getLeader().isEmpty()) {
                    // An island file that cannot be parsed loads as an island without a party, and unlocked, so its
                    // nether region stays as it is rather than lose its lock. protectIsland sets it up again.
                    unreadable++;
                    logger.warning("Island " + islandName + " has no party leader, so its file may not have been"
                        + " read; its nether region is left as it is.");
                } else {
                    ProtectedRegion netherRegion = regions.netherRegionOf(islandName);
                    if (netherRegion != null) {
                        WorldGuardHandler.updateLockStatus(netherRegion, island.isLocked());
                        synced++;
                    }
                }
            } catch (RuntimeException e) {
                unreadable++;
                logger.log(Level.WARNING, "Unable to sync the nether region of island " + islandName
                    + " to its lock", e);
            } finally {
                if (!wasInMemory) {
                    islandLogic.removeIslandFromMemory(islandName);
                }
            }
        }

        /**
         * Removes the island's nether region if the island is gone: its file is definitely absent, not merely
         * unreadable, and its region in the island world went with it, as deleting an island left them.
         */
        private void removeIfOrphaned(String islandName) {
            // An island deleted while the repair runs has taken its nether region with it already.
            if (Files.notExists(islandFile(islandName)) && !regions.hasIslandRegion(islandName)
                && regions.removeNetherRegion(islandName)) {
                removed++;
                logger.info("Removed the nether region " + islandName + "nether: its island no longer exists.");
            }
        }

        private void finish() {
            logger.info("Island region repair done: synced " + synced + " nether regions to their island's lock"
                + (unreadable > 0 ? " (" + unreadable + " islands could not be read)" : "") + ", and removed "
                + removed + " nether regions of deleted islands. Its marker follows in " + MARKER_DELAY.toMinutes()
                + " minutes, once WorldGuard has saved the regions.");
            task = scheduler.sync(this::leaveMarker, MARKER_DELAY);
        }

        private void leaveMarker() {
            try {
                Files.createDirectories(markerFile().getParent());
                Files.writeString(markerFile(), "uSkyBlock synced the nether regions to their islands' locks and"
                    + " removed those of deleted islands on " + Instant.now() + ".\n"
                    + "Delete this file to run that again at the next start.\n");
                logger.info("Island region repair: left its marker " + markerFile() + ", so it does not run again.");
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to write " + markerFile() + ", so the nether regions are synced"
                    + " again at the next start", e);
            }
        }
    }

    /**
     * Where the repair finds the island regions it reads and changes. WorldGuard's region managers stay behind it, out
     * of the unit tests' reach.
     */
    interface IslandRegions {
        boolean available();

        /**
         * @return the ids of all regions in the nether island world
         */
        Set<String> netherRegionIds();

        /**
         * @return whether the island has a region in the island world
         */
        boolean hasIslandRegion(String islandName);

        /**
         * @return the island's region in the nether island world, or null without one
         */
        ProtectedRegion netherRegionOf(String islandName);

        /**
         * @return whether there was a region to remove
         */
        boolean removeNetherRegion(String islandName);
    }

    private record WorldGuardRegions(uSkyBlock plugin) implements IslandRegions {
        @Override
        public boolean available() {
            return plugin.getServer().getPluginManager().isPluginEnabled("WorldGuard");
        }

        @Override
        public Set<String> netherRegionIds() {
            return WorldGuardHandler.getNetherRegionIds();
        }

        @Override
        public boolean hasIslandRegion(String islandName) {
            return WorldGuardHandler.hasIslandRegion(islandName);
        }

        @Override
        public ProtectedRegion netherRegionOf(String islandName) {
            return WorldGuardHandler.getNetherRegion(islandName);
        }

        @Override
        public boolean removeNetherRegion(String islandName) {
            return WorldGuardHandler.removeNetherRegion(islandName);
        }
    }
}
