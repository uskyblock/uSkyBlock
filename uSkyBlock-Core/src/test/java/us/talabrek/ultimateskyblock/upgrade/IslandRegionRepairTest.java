package us.talabrek.ultimateskyblock.upgrade;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
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
        assertTrue(logged.contains("Synced 4 regions of 2 islands to their lock, owners and members, and removed 1 nether"
            + " regions of deleted islands."), logged.toString());
        assertTrue(Files.exists(marker()));
    }

    @Test
    public void runsOnlyOnce() throws IOException {
        islandFiles("128,0");
        Repair first = new Repair();
        first.start();
        first.runToTheEnd();

        Scheduler scheduler = mock(Scheduler.class);
        new IslandRegionRepair(dataDir, logger, plugin, islandLogic, runtimeConfigs, scheduler, regions).start();

        verifyNoInteractions(scheduler);
        assertEquals(List.of("128,0"), regions.synced);
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
    public void keepsTheNetherRegionOfAnIslandThatCannotBeRead() throws IOException {
        islandFiles("128,0", "256,0");
        regions.netherRegionIds.addAll(Set.of("128,0nether", "256,0nether"));
        when(islandLogic.getIslandInfo("128,0")).thenThrow(new IllegalStateException("Unable to load island"));

        Repair repair = new Repair();
        repair.start();
        repair.runToTheEnd();

        assertEquals(List.of("256,0"), regions.synced);
        assertEquals(Set.of("128,0nether", "256,0nether"), regions.netherRegionIds);
        assertTrue(logged.contains("Unable to sync the regions of island 128,0 to the island"), logged.toString());
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
        assertTrue(logged.contains("Island 128,0 has no party leader, so its file may not have been read; its regions"
            + " are left as they are."), logged.toString());
        assertTrue(logged.contains("Synced 1 regions of 1 islands to their lock, owners and members (1 islands could"
            + " not be read), and removed 0 nether regions of deleted islands."), logged.toString());
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
        assertEquals(List.of("128,0", "256,0"), regions.synced);
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
        assertEquals(islands.size(), regions.synced.size());
        assertTrue(Files.exists(marker()));
    }

    private void islandFiles(String... names) throws IOException {
        for (String name : names) {
            Files.writeString(dataDir.resolve("islands").resolve(name + ".yml"), "general: {}\n");
        }
    }

    private Path marker() {
        return dataDir.resolve("data").resolve(IslandRegionRepair.MARKER_FILE);
    }

    private static IslandInfo island(String name) {
        IslandInfo island = mock(IslandInfo.class);
        when(island.getName()).thenReturn(name);
        when(island.getLeader()).thenReturn("UsbIt");
        return island;
    }

    /**
     * The repair with a scheduler whose ticks the test runs.
     */
    private final class Repair {
        private final Scheduler scheduler = mock(Scheduler.class);
        private final ArgumentCaptor<Runnable> scheduled = ArgumentCaptor.forClass(Runnable.class);
        private boolean cancelled;

        private Repair() {
            BukkitTask task = mock(BukkitTask.class);
            doAnswer(invocation -> cancelled = true).when(task).cancel();
            when(scheduler.sync(scheduled.capture(), any(Duration.class), any(Duration.class))).thenReturn(task);
        }

        void start() {
            new IslandRegionRepair(dataDir, logger, plugin, islandLogic, runtimeConfigs, scheduler, regions).start();
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
    }

    private static final class FakeRegions implements IslandRegionRepair.IslandRegions {
        private boolean available = true;
        /** The islands with a region in the island world. */
        private final Set<String> islandRegions = new HashSet<>();
        private final Set<String> netherRegionIds = new HashSet<>();
        private final List<String> synced = new ArrayList<>();

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
        public int sync(IslandInfo island) {
            synced.add(island.getName());
            return (islandRegions.contains(island.getName()) ? 1 : 0)
                + (netherRegionIds.contains(island.getName() + "nether") ? 1 : 0);
        }

        @Override
        public void removeNetherRegion(String islandName) {
            netherRegionIds.remove(islandName + "nether");
        }
    }
}
