package us.talabrek.ultimateskyblock.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
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
 * Brings the island regions of a server in line with its islands, once. Before, {@code /is lock} and
 * {@code /is unlock} changed only an island's region in the island world, so its nether region kept the lock it had
 * when it was last rebuilt; and deleting an island left its nether region behind, still locked and owned by its last
 * party. The first start with this version gives every island's regions the island's lock, owners and members, removes
 * the nether regions of islands that no longer exist, and leaves a marker so it never runs again. It works through a
 * few islands each tick, so a server with thousands of them does not stall.
 */
@Singleton
public class IslandRegionRepair {
    /** Under the plugin's {@code data/} folder: there once the repair has run. */
    static final String MARKER_FILE = "island-region-repair.done";
    /** How many islands, or nether regions, the repair works through in one tick. */
    static final int PER_TICK = 20;
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
            logger.info("WorldGuard is not enabled, so the island regions are synced to their islands at a later start.");
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
        logger.info("Syncing the regions of " + islands.size() + " islands to their lock, owners and members, and"
            + " removing the nether regions of deleted islands. This runs once, " + PER_TICK + " islands a tick.");
        run = new Run(islands.iterator(), netherIslands.iterator());
        run.task = scheduler.sync(run, Duration.ZERO, Duration.ofMillis(50));
    }

    /**
     * Stops a repair still running, at a reload or a stop: it uses the services of this start, and the next start
     * runs it again from the beginning, as it has left no marker.
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
        private BukkitTask task;
        private int islandsSynced;
        private int regionsSynced;
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
                    // An island file that cannot be parsed loads as an island without a party, so its regions
                    // stay as they are rather than lose their owners and lock. protectIsland sets them up again.
                    unreadable++;
                    logger.warning("Island " + islandName + " has no party leader, so its file may not have been"
                        + " read; its regions are left as they are.");
                } else {
                    regionsSynced += regions.sync(island);
                    islandsSynced++;
                }
            } catch (RuntimeException e) {
                unreadable++;
                logger.log(Level.WARNING, "Unable to sync the regions of island " + islandName + " to the island", e);
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
            logger.info("Synced " + regionsSynced + " regions of " + islandsSynced + " islands to their lock, owners"
                + " and members" + (unreadable > 0 ? " (" + unreadable + " islands could not be read)" : "")
                + ", and removed " + removed + " nether regions of deleted islands.");
            // WorldGuard writes changed regions only every 30 seconds, so the marker waits for them to be written: a
            // crash before then must not leave the marker of a lost repair. On the main thread: it runs once, and there
            // no command changes the regions while they are written.
            saveAndLeaveMarker();
        }

        private void saveAndLeaveMarker() {
            try {
                regions.save();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to save the island regions, so they are synced again at the next"
                    + " start", e);
                return;
            }
            try {
                Files.createDirectories(markerFile().getParent());
                Files.writeString(markerFile(), "uSkyBlock synced the island regions to their islands and removed the"
                    + " nether regions of deleted islands on " + Instant.now() + ".\n"
                    + "Delete this file to run that again at the next start.\n");
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to write " + markerFile() + ", so the island regions are synced"
                    + " again at the next start", e);
            }
        }
    }

    /**
     * The island regions the repair reads and changes. WorldGuard stays behind it, out of the unit tests' reach.
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
         * Gives the island's regions the island's lock, owners and members.
         *
         * @return the number of regions synced
         */
        int sync(IslandInfo island);

        /**
         * @return whether there was a region to remove
         */
        boolean removeNetherRegion(String islandName);

        /**
         * Writes the changed regions to WorldGuard's storage now.
         */
        void save() throws IOException;
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
        public int sync(IslandInfo island) {
            return WorldGuardHandler.syncRegions(island);
        }

        @Override
        public boolean removeNetherRegion(String islandName) {
            return WorldGuardHandler.removeNetherRegion(islandName);
        }

        @Override
        public void save() throws IOException {
            WorldGuardHandler.saveRegions();
        }
    }
}
