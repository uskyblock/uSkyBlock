package us.talabrek.ultimateskyblock.upgrade;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigFactory;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.island.IslandLogic;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class IslandRegionRepairTest {
    @TempDir
    Path dataDir;

    private final List<String> logged = new ArrayList<>();
    private final FakeRegions regions = new FakeRegions();
    private uSkyBlock plugin;
    private IslandLogic islandLogic;
    private RuntimeConfigs runtimeConfigs;
    private Logger logger;

    @BeforeEach
    public void setUp() throws Exception {
        Files.createDirectories(dataDir.resolve("islands"));
        plugin = mock(uSkyBlock.class);
        islandLogic = mock(IslandLogic.class);
        when(islandLogic.getIslandInfo(anyString())).thenAnswer(invocation -> island(invocation.getArgument(0)));
        BukkitServerMock.setupServerMock();
        YamlConfiguration config = new YamlConfiguration();
        config.setDefaults(PluginConfigLoader.loadBundledConfig());
        RuntimeConfig runtimeConfig = new RuntimeConfigFactory(new GameObjectFactory(), Logger.getAnonymousLogger()).load(config);
        runtimeConfigs = mock(RuntimeConfigs.class);
        when(runtimeConfigs.current()).thenReturn(runtimeConfig);
        logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                logged.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
    }

    @Test
    public void syncsEveryIslandAndRemovesOnlyTheNetherRegionsOfDeletedIslands() throws IOException {
        islandFiles("128,0", "-256,128");
        Files.writeString(dataDir.resolve("islands").resolve("notes.txt"), "not an island");
        regions.islandRegions.addAll(Set.of("128,0", "-256,128", "640,0"));
        regions.netherRegionIds.addAll(Set.of("128,0nether", "-256,128nether",
            // Deleted: the island file and the region in the island world are gone.
            "512,-512nether",
            // The island file is gone but its region in the island world is not, so the island may only be missing.
            "640,0nether",
            // Not island nether regions.
            "__global__", "spawn", "notchnether", "notchisland"));

        Repair repair = new Repair();
        repair.start();
        repair.runToTheEnd();

        assertEquals(List.of("-256,128", "128,0"), regions.synced);
        assertEquals(Set.of("128,0nether", "-256,128nether", "640,0nether", "__global__", "spawn", "notchnether", "notchisland"),
            regions.netherRegionIds);
        assertTrue(logged.contains("Removed the nether region 512,-512nether: its island no longer exists."), logged.toString());
        assertTrue(logged.contains(done("synced 2 nether regions to their island's lock, and removed 1"
            + " nether regions of deleted islands")), logged.toString());
    }

    @Test
    public void leavesTheOwnersAndMembersOfTheRegionsAsTheyAre() throws IOException {
        islandFiles("128,0");
        // What uSkyBlock's old name-to-UUID conversion stored on an offline-mode server: not the owner's real UUID.
        UUID offlineUuid = UUID.nameUUIDFromBytes("OfflinePlayer:Owner".getBytes(StandardCharsets.UTF_8));
        UUID realUuid = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
        UUID handAdded = UUID.fromString("853c80ef-3c37-49fd-aa49-938b674adae6");
        IslandInfo island = island("128,0");
        when(island.getMemberUUIDs()).thenReturn(Set.of(offlineUuid));
        when(island.getTrusteeUUIDs()).thenReturn(List.of());
        when(islandLogic.getIslandInfo("128,0")).thenReturn(island);
        // Owners other than the island file's: the owner's real UUID, and a name as WorldGuard's own migration left it.
        ProtectedRegion region = regions.islandRegion("128,0");
        region.getOwners().addPlayer(realUuid);
        region.getOwners().addPlayer("owner");
        // An owner and a member added with /rg addowner and /rg addmember.
        ProtectedRegion netherRegion = regions.netherRegion("128,0");
        netherRegion.getOwners().addPlayer(offlineUuid);
        netherRegion.getOwners().addPlayer(handAdded);
        netherRegion.getMembers().addPlayer(handAdded);

        Repair repair = new Repair();
        repair.start();
        repair.runToTheEnd();

        assertDomain(Set.of(realUuid), Set.of("owner"), region.getOwners());
        assertDomain(Set.of(), Set.of(), region.getMembers());
        assertDomain(Set.of(offlineUuid, handAdded), Set.of(), netherRegion.getOwners());
        assertDomain(Set.of(handAdded), Set.of(), netherRegion.getMembers());
        assertTrue(logged.contains(done("synced 1 nether regions to their island's lock, and removed 0 nether regions"
            + " of deleted islands")), logged.toString());
    }

    @Test
    public void syncsTheLockOfTheNetherRegionsAndLeavesTheRegionsInTheIslandWorld() throws IOException {
        islandFiles("128,0", "256,0");
        IslandInfo locked = island("128,0");
        when(locked.isLocked()).thenReturn(true);
        when(islandLogic.getIslandInfo("128,0")).thenReturn(locked);
        // Locks set by hand with /rg flag, other than the island's: the repair leaves them in the island world.
        ProtectedRegion lockedRegion = regions.islandRegion("128,0");
        ProtectedRegion lockedNetherRegion = regions.netherRegion("128,0");
        ProtectedRegion unlockedRegion = regions.islandRegion("256,0");
        unlockedRegion.setFlag(Flags.ENTRY, StateFlag.State.DENY);
        ProtectedRegion unlockedNetherRegion = regions.netherRegion("256,0");
        unlockedNetherRegion.setFlag(Flags.ENTRY, StateFlag.State.DENY);
        // A flag set by hand.
        unlockedNetherRegion.setFlag(Flags.PVP, StateFlag.State.ALLOW);

        Repair repair = new Repair();
        repair.start();
        repair.runToTheEnd();

        assertNull(lockedRegion.getFlag(Flags.ENTRY));
        assertEquals(StateFlag.State.DENY, lockedNetherRegion.getFlag(Flags.ENTRY));
        assertEquals(StateFlag.State.DENY, unlockedRegion.getFlag(Flags.ENTRY));
        assertNull(unlockedNetherRegion.getFlag(Flags.ENTRY));
        assertEquals(StateFlag.State.ALLOW, unlockedNetherRegion.getFlag(Flags.PVP));
        assertTrue(logged.contains(done("synced 2 nether regions to their island's lock, and removed 0 nether regions"
            + " of deleted islands")), logged.toString());
    }

    @Test
    public void doesNotCountANetherRegionThatADeleteRemovedWhileItRuns() throws IOException {
        regions.netherRegionIds.add("512,-512nether");
        Repair repair = new Repair();
        repair.start();

        // The island's delete removes its nether region after the repair has listed the regions.
        regions.netherRegionIds.remove("512,-512nether");
        repair.runToTheEnd();

        assertFalse(logged.contains("Removed the nether region 512,-512nether: its island no longer exists."),
            logged.toString());
        assertTrue(logged.contains(done("synced 0 nether regions to their island's lock, and removed 0"
            + " nether regions of deleted islands")), logged.toString());
    }

    @Test
    public void leavesItsMarkerOnlyAfterTheDelay() throws IOException {
        islandFiles("128,0");
        Repair repair = new Repair();
        repair.start();
        repair.runToTheEnd();

        // The repair has no save of its own: WorldGuard saves the regions by itself, well before the marker follows.
        assertTrue(logged.contains(done("synced 0 nether regions to their island's lock, and removed 0"
            + " nether regions of deleted islands")), logged.toString());
        assertFalse(Files.exists(marker()));
        assertEquals(Duration.ofMinutes(2), repair.markerDelay());

        repair.afterTheMarkerDelay();
        assertTrue(Files.exists(marker()));
        assertTrue(logged.contains("Island region repair: left its marker " + marker() + ", so it does not run again."),
            logged.toString());
    }

    @Test
    public void aStopOrAReloadBeforeTheMarkerLeavesNoneAndTheNextStartRunsAgain() throws IOException {
        islandFiles("128,0");
        Repair stopped = new Repair();
        stopped.start();
        stopped.runToTheEnd();

        stopped.shutdown();
        assertTrue(stopped.markerCancelled);
        assertFalse(Files.exists(marker()));

        Repair next = new Repair();
        next.start();
        next.runToTheEnd();
        assertEquals(List.of("128,0", "128,0"), regions.synced);
        next.afterTheMarkerDelay();
        assertTrue(Files.exists(marker()));
    }

    @Test
    public void aSecondRunChangesNothing() throws IOException {
        islandFiles("128,0");
        regions.islandRegions.add("128,0");
        regions.netherRegionIds.addAll(Set.of("128,0nether", "512,-512nether", "spawn"));
        Repair first = new Repair();
        first.start();
        first.runToTheEnd();
        first.shutdown();
        Set<String> afterTheFirstRun = regions.netherRegionIds();
        assertEquals(Set.of("128,0nether", "spawn"), afterTheFirstRun);
        logged.clear();

        Repair second = new Repair();
        second.start();
        second.runToTheEnd();

        assertEquals(afterTheFirstRun, regions.netherRegionIds());
        assertEquals(List.of("128,0", "128,0"), regions.synced);
        assertTrue(logged.contains(done("synced 1 nether regions to their island's lock, and removed 0"
            + " nether regions of deleted islands")), logged.toString());
        assertTrue(logged.stream().noneMatch(line -> line.startsWith("Removed the nether region")), logged.toString());
    }

    @Test
    public void runsOnlyOnce() throws IOException {
        islandFiles("128,0");
        Repair first = new Repair();
        first.start();
        first.runToTheEnd();
        first.afterTheMarkerDelay();

        Scheduler scheduler = mock(Scheduler.class);
        new IslandRegionRepair(dataDir, logger, plugin, islandLogic, runtimeConfigs, scheduler, regions).start();

        verifyNoInteractions(scheduler);
        assertEquals(List.of("128,0"), regions.synced);
    }

    @Test
    public void doesNotRunWhereTheRepairOf370Ran() throws IOException {
        islandFiles("128,0");
        // 3.7.0's marker, by its name in 3.7.0.
        Files.createDirectories(dataDir.resolve("data"));
        Files.writeString(dataDir.resolve("data").resolve("island-region-repair.done"), "");

        Scheduler scheduler = mock(Scheduler.class);
        new IslandRegionRepair(dataDir, logger, plugin, islandLogic, runtimeConfigs, scheduler, regions).start();

        verifyNoInteractions(scheduler);
        assertEquals(List.of(), regions.synced);
    }

    @Test
    public void waitsForAStartWithWorldGuard() throws IOException {
        islandFiles("128,0");
        regions.available = false;
        Scheduler scheduler = mock(Scheduler.class);

        new IslandRegionRepair(dataDir, logger, plugin, islandLogic, runtimeConfigs, scheduler, regions).start();

        verifyNoInteractions(scheduler);
        assertFalse(Files.exists(marker()));
    }

    @Test
    public void waitsForTheNextStartIfTheIslandsCannotBeListed() throws IOException {
        islandFiles("128,0");
        Path islands = dataDir.resolve("islands");
        Scheduler scheduler = mock(Scheduler.class);

        // A read error part-way through the listing, which the stream reports unchecked.
        try (MockedStatic<Files> files = mockStatic(Files.class, CALLS_REAL_METHODS)) {
            files.when(() -> Files.newDirectoryStream(islands)).thenReturn(failingListing());
            new IslandRegionRepair(dataDir, logger, plugin, islandLogic, runtimeConfigs, scheduler, regions).start();
        }

        verifyNoInteractions(scheduler);
        assertFalse(Files.exists(marker()));
        assertTrue(logged.contains("Unable to list the islands, so their regions are synced at the next start"),
            logged.toString());
    }

    @Test
    public void keepsTheNetherRegionOfAnIslandThatCannotBeRead() throws IOException {
        islandFiles("128,0", "256,0");
        regions.netherRegionIds.addAll(Set.of("128,0nether", "256,0nether"));
        when(islandLogic.getIslandInfo("128,0")).thenThrow(new IllegalStateException("Unable to load island"));

        Repair repair = new Repair();
        repair.start();
        repair.runToTheEnd();

        assertEquals(List.of("256,0"), regions.synced);
        assertEquals(Set.of("128,0nether", "256,0nether"), regions.netherRegionIds);
        assertTrue(logged.contains("Unable to sync the nether region of island 128,0 to its lock"), logged.toString());
    }

    @Test
    public void keepsTheRegionsOfAnIslandWhoseFileCouldNotBeParsed() throws IOException {
        islandFiles("256,0");
        // Truncated by a crash: the island loads without a party, as if its file were empty.
        Files.writeString(dataDir.resolve("islands").resolve("128,0.yml"), "party:\n  leader: UsbIt\n  members: [ab");
        regions.netherRegionIds.addAll(Set.of("128,0nether", "256,0nether"));
        IslandInfo unparsed = island("128,0");
        when(unparsed.getLeader()).thenReturn("");
        when(islandLogic.getIslandInfo("128,0")).thenReturn(unparsed);

        Repair repair = new Repair();
        repair.start();
        repair.runToTheEnd();

        assertEquals(List.of("256,0"), regions.synced);
        assertEquals(Set.of("128,0nether", "256,0nether"), regions.netherRegionIds);
        assertTrue(logged.contains("Island 128,0 has no party leader, so its file may not have been read; its nether"
            + " region is left as it is."), logged.toString());
        assertTrue(logged.contains(done("synced 1 nether regions to their island's lock (1 islands could not be read),"
            + " and removed 0 nether regions of deleted islands")), logged.toString());
    }

    @Test
    public void passesOverAnIslandDeletedWhileItRuns() throws IOException {
        islandFiles("128,0", "256,0");
        Repair repair = new Repair();
        repair.start();

        // The island's delete removes its file and its regions after the repair has listed the islands.
        Files.delete(dataDir.resolve("islands").resolve("128,0.yml"));
        repair.runToTheEnd();

        assertEquals(List.of("256,0"), regions.synced);
        verify(islandLogic, never()).getIslandInfo("128,0");
        assertTrue(logged.contains(done("synced 0 nether regions to their island's lock, and removed 0"
            + " nether regions of deleted islands")), logged.toString());
    }

    @Test
    public void takesTheIslandsItLoadedOutOfMemoryAgain() throws IOException {
        islandFiles("128,0", "256,0", "384,0");
        // A player on the server has this island in memory.
        when(islandLogic.isIslandInMemory("256,0")).thenReturn(true);
        when(islandLogic.getIslandInfo("384,0")).thenThrow(new IllegalStateException("Unable to load island"));

        Repair repair = new Repair();
        repair.start();
        repair.runToTheEnd();

        verify(islandLogic).removeIslandFromMemory("128,0");
        verify(islandLogic, never()).removeIslandFromMemory("256,0");
        verify(islandLogic).removeIslandFromMemory("384,0");
    }

    @Test
    public void waitsWhileTheServerIsInMaintenanceMode() throws IOException {
        islandFiles("128,0", "256,0");
        Repair repair = new Repair();
        repair.start();

        when(plugin.isMaintenanceMode()).thenReturn(true);
        repair.tick();
        repair.tick();
        assertEquals(List.of(), regions.synced);
        assertFalse(Files.exists(marker()));

        when(plugin.isMaintenanceMode()).thenReturn(false);
        repair.runToTheEnd();
        repair.afterTheMarkerDelay();
        assertEquals(List.of("128,0", "256,0"), regions.synced);
        assertTrue(Files.exists(marker()));
    }

    @Test
    public void stopsAtAReloadAndRunsAgainAtTheNextStart() throws IOException {
        List<String> islands = new ArrayList<>();
        for (int i = 0; i < IslandRegionRepair.PER_TICK + 5; i++) {
            islands.add((i + 1) * 128 + ",0");
        }
        islandFiles(islands.toArray(String[]::new));
        Repair stopped = new Repair();
        stopped.start();
        stopped.tick();

        stopped.shutdown();
        assertTrue(stopped.cancelled);
        assertFalse(Files.exists(marker()));

        Repair next = new Repair();
        next.start();
        next.runToTheEnd();
        next.afterTheMarkerDelay();
        assertEquals(IslandRegionRepair.PER_TICK + islands.size(), regions.synced.size());
        assertTrue(Files.exists(marker()));
    }

    @Test
    public void worksThroughAFewIslandsEachTick() throws IOException {
        List<String> islands = new ArrayList<>();
        for (int i = 0; i < 2 * IslandRegionRepair.PER_TICK + 5; i++) {
            islands.add((i + 1) * 128 + ",0");
        }
        islandFiles(islands.toArray(String[]::new));
        Repair repair = new Repair();
        repair.start();

        repair.tick();
        assertEquals(IslandRegionRepair.PER_TICK, regions.synced.size());
        repair.tick();
        assertEquals(2 * IslandRegionRepair.PER_TICK, regions.synced.size());
        assertFalse(Files.exists(marker()));
        repair.runToTheEnd();
        repair.afterTheMarkerDelay();
        assertEquals(islands.size(), regions.synced.size());
        assertTrue(Files.exists(marker()));
    }

    private void islandFiles(String... names) throws IOException {
        for (String name : names) {
            Files.writeString(dataDir.resolve("islands").resolve(name + ".yml"), "general: {}\n");
        }
    }

    private static DirectoryStream<Path> failingListing() {
        return new DirectoryStream<>() {
            @Override
            public Iterator<Path> iterator() {
                return new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        throw new DirectoryIteratorException(new IOException("Input/output error"));
                    }

                    @Override
                    public Path next() {
                        throw new NoSuchElementException();
                    }
                };
            }

            @Override
            public void close() {
            }
        };
    }

    private static void assertDomain(Set<UUID> uuids, Set<String> names, DefaultDomain domain) {
        assertEquals(uuids, domain.getUniqueIds());
        assertEquals(names, domain.getPlayers());
        assertEquals(Set.of(), domain.getGroups());
    }

    private Path marker() {
        return dataDir.resolve("data").resolve(IslandRegionRepair.MARKER_FILE);
    }

    /**
     * @return the line the repair logs when it is done, with its counts
     */
    private static String done(String counts) {
        return "Island region repair done: " + counts + ". Its marker follows in 2 minutes, once WorldGuard has saved"
            + " the regions.";
    }

    private static IslandInfo island(String name) {
        IslandInfo island = mock(IslandInfo.class);
        when(island.getName()).thenReturn(name);
        when(island.getLeader()).thenReturn("UsbIt");
        return island;
    }

    /**
     * The repair with a scheduler whose ticks, and whose marker after its delay, the test runs.
     */
    private final class Repair {
        private final Scheduler scheduler = mock(Scheduler.class);
        private final ArgumentCaptor<Runnable> scheduled = ArgumentCaptor.forClass(Runnable.class);
        private final ArgumentCaptor<Runnable> marker = ArgumentCaptor.forClass(Runnable.class);
        private final ArgumentCaptor<Duration> markerDelay = ArgumentCaptor.forClass(Duration.class);
        private boolean cancelled;
        private boolean markerCancelled;
        private IslandRegionRepair repair;

        private Repair() {
            BukkitTask task = mock(BukkitTask.class);
            doAnswer(invocation -> cancelled = true).when(task).cancel();
            when(scheduler.sync(scheduled.capture(), any(Duration.class), any(Duration.class))).thenReturn(task);
            BukkitTask markerTask = mock(BukkitTask.class);
            doAnswer(invocation -> markerCancelled = true).when(markerTask).cancel();
            when(scheduler.sync(marker.capture(), markerDelay.capture())).thenReturn(markerTask);
        }

        void start() {
            repair = new IslandRegionRepair(dataDir, logger, plugin, islandLogic, runtimeConfigs, scheduler, regions);
            repair.start();
        }

        void shutdown() {
            repair.shutdown();
        }

        void tick() {
            scheduled.getValue().run();
        }

        void runToTheEnd() {
            for (int ticks = 0; !cancelled; ticks++) {
                assertTrue(ticks < 100, "the repair does not end");
                tick();
            }
        }

        Duration markerDelay() {
            return markerDelay.getValue();
        }

        /**
         * Runs the marker the repair left to follow it, as the scheduler does once its delay has passed.
         */
        void afterTheMarkerDelay() {
            assertFalse(markerCancelled, "the marker was cancelled");
            marker.getValue().run();
        }
    }

    private static final class FakeRegions implements IslandRegionRepair.IslandRegions {
        private boolean available = true;
        /** The islands with a region in the island world. */
        private final Set<String> islandRegions = new HashSet<>();
        private final Set<String> netherRegionIds = new HashSet<>();
        /** The regions of both worlds by id, made the first time they are asked for. */
        private final Map<String, ProtectedRegion> regions = new HashMap<>();
        /** The islands whose nether region the repair asked for, to sync it. */
        private final List<String> synced = new ArrayList<>();

        /**
         * @return the island's region in the island world, which the island has from now on
         */
        ProtectedRegion islandRegion(String islandName) {
            islandRegions.add(islandName);
            return region(islandName + "island");
        }

        /**
         * @return the island's nether region, which the island has from now on
         */
        ProtectedRegion netherRegion(String islandName) {
            netherRegionIds.add(islandName + "nether");
            return region(islandName + "nether");
        }

        private ProtectedRegion region(String id) {
            return regions.computeIfAbsent(id,
                key -> new ProtectedCuboidRegion(key, BlockVector3.at(0, 0, 0), BlockVector3.at(127, 255, 127)));
        }

        @Override
        public boolean available() {
            return available;
        }

        @Override
        public Set<String> netherRegionIds() {
            return Set.copyOf(netherRegionIds);
        }

        @Override
        public boolean hasIslandRegion(String islandName) {
            return islandRegions.contains(islandName);
        }

        @Override
        public ProtectedRegion netherRegionOf(String islandName) {
            synced.add(islandName);
            return netherRegionIds.contains(islandName + "nether") ? region(islandName + "nether") : null;
        }

        @Override
        public boolean removeNetherRegion(String islandName) {
            regions.remove(islandName + "nether");
            return netherRegionIds.remove(islandName + "nether");
        }
    }
}
