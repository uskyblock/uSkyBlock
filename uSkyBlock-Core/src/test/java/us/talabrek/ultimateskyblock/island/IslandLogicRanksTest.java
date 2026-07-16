package us.talabrek.ultimateskyblock.island;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.api.IslandLevel;
import us.talabrek.ultimateskyblock.api.IslandRank;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigFactory;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.island.level.IslandScore;
import us.talabrek.ultimateskyblock.player.TeleportLogic;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.Scheduler;
import us.talabrek.ultimateskyblock.uuid.PlayerDB;
import us.talabrek.ultimateskyblock.world.WorldManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Focused unit tests for the in-memory ranking operations of {@link IslandLogic}:
 * {@link IslandLogic#updateRank}, {@link IslandLogic#getRank(String)} and
 * {@link IslandLogic#getRanks(int, int)}.
 *
 * <p>These operate purely on the internal {@code ranks} list, so we construct a real
 * {@link IslandLogic} with mocked collaborators and a temp data directory. The list is
 * seeded through the public {@code updateRank} method (there is no setter), which also
 * exercises the add/replace/sort behaviour.
 */
public class IslandLogicRanksTest {

    private IslandLogic islandLogic;

    @BeforeEach
    public void setUp() throws Exception {
        BukkitServerMock.setupServerMock();

        // Minimal config that the RuntimeConfigFactory can fully load (mirrors InternalEventsTest).
        YamlConfiguration config = new YamlConfiguration();
        config.setDefaults(PluginConfigLoader.loadBundledConfig());
        config.set("language", "en");
        config.set("options.general.worldName", "skyworld");
        config.set("options.general.cooldownRestart", "1m");
        config.set("options.general.biomeChange", "1m");
        config.set("options.general.defaultBiome", "plains");
        config.set("options.general.defaultNetherBiome", "nether_wastes");
        config.set("options.advanced.confirmTimeout", "30s");
        config.set("options.advanced.playerdb.storage", "file");
        config.set("options.advanced.playerdb.nameCache", "maximumSize=100");
        config.set("options.advanced.playerdb.uuidCache", "maximumSize=100");
        config.set("options.advanced.playerCache", "maximumSize=100");
        config.set("options.advanced.islandCache", "maximumSize=100");
        config.set("options.advanced.island.saveEvery", 60);
        config.set("options.advanced.player.saveEvery", 60);
        config.set("options.party.invite-timeout", "1m");
        config.set("options.island.distance", 110);
        config.set("options.island.height", 120);
        config.set("options.island.topTenTimeout", "15m");
        config.set("options.island.islandTeleportDelay", "2s");
        config.set("options.island.chat-format", "default");
        config.set("options.party.chat-format", "default");
        config.set("nether.chunk-generator", "default");
        config.set("plugin-updates.branch", "LATEST");

        // Pre-evaluate the factory load: it touches the mocked Bukkit server, so evaluating it
        // inside when(...).thenReturn(...) while a stubbing is in progress corrupts Mockito state.
        RuntimeConfig runtimeConfig = new RuntimeConfigFactory(new GameObjectFactory(), Logger.getAnonymousLogger())
            .load(config);
        RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
        doReturn(runtimeConfig).when(runtimeConfigs).current();

        Path dataDir = Files.createTempDirectory("islandlogic-ranks-test");

        islandLogic = new IslandLogic(
            Logger.getAnonymousLogger(),
            mock(uSkyBlock.class),
            mock(WorldManager.class),
            mock(TeleportLogic.class),
            mock(Scheduler.class),
            runtimeConfigs,
            dataDir,
            mock(OrphanLogic.class),
            mock(PlayerDB.class)
        );
    }

    /**
     * Seeds one island via the public updateRank path. Distinct scores keep the sort order
     * deterministic (IslandLevel ties would fall back to leader/island name).
     */
    private void addIsland(String islandName, String leader, double score) {
        IslandInfo info = mock(IslandInfo.class);
        when(info.getName()).thenReturn(islandName);
        when(info.getLeader()).thenReturn(leader);
        when(info.getMembers()).thenReturn(Set.of(leader));
        islandLogic.updateRank(info, new IslandScore(score, Collections.emptyList()));
    }

    @Test
    public void updateRankSortsByScoreDescending() {
        addIsland("e", "eve", 25);
        addIsland("a", "alice", 300);
        addIsland("c", "carol", 100);

        List<IslandLevel> ranks = islandLogic.getRanks(0, 3);
        assertEquals(3, ranks.size());
        assertEquals("a", ranks.get(0).getIslandName());
        assertEquals("c", ranks.get(1).getIslandName());
        assertEquals("e", ranks.get(2).getIslandName());
        assertEquals(300d, ranks.get(0).getScore(), 1e-9);
    }

    @Test
    public void updateRankReplacesIslandWithSameName() {
        addIsland("a", "alice", 100);
        addIsland("b", "bob", 200);
        // Re-rank "a" with a higher score: equals() is by island name, so the old entry is replaced.
        addIsland("a", "alice", 500);

        List<IslandLevel> ranks = islandLogic.getRanks(0, 100);
        assertEquals(2, ranks.size());
        assertEquals("a", ranks.get(0).getIslandName());
        assertEquals(500d, ranks.get(0).getScore(), 1e-9);
        assertEquals("b", ranks.get(1).getIslandName());
        assertEquals(1, islandLogic.getRank("a").getRank());
    }

    @Test
    public void getRankIsOneBasedCaseInsensitiveAndNullForMissing() {
        addIsland("a", "alice", 300);
        addIsland("b", "bob", 200);
        addIsland("c", "carol", 100);
        addIsland("d", "dave", 50);
        addIsland("e", "eve", 25);

        IslandRank first = islandLogic.getRank("a");
        assertEquals(1, first.getRank());
        assertEquals("a", first.getIslandName());

        // 1-based and case-insensitive.
        assertEquals(4, islandLogic.getRank("D").getRank());
        assertEquals(5, islandLogic.getRank("e").getRank());

        assertNull(islandLogic.getRank("does-not-exist"));
    }

    @Test
    public void getRanksReturnsWindowFromStartAndEmptyBeyondSize() {
        addIsland("a", "alice", 300);
        addIsland("b", "bob", 200);
        addIsland("c", "carol", 100);
        addIsland("d", "dave", 50);
        addIsland("e", "eve", 25);

        // offset 0 is the well-behaved regime: first `length` entries.
        List<IslandLevel> window = islandLogic.getRanks(0, 2);
        assertEquals(2, window.size());
        assertEquals("a", window.get(0).getIslandName());
        assertEquals("b", window.get(1).getIslandName());

        // offset >= size short-circuits to an empty list.
        assertTrue(islandLogic.getRanks(5, 3).isEmpty());
        assertTrue(islandLogic.getRanks(10, 3).isEmpty());
    }

    @Test
    public void getRanksOffByOneOnNonZeroOffset() {
        addIsland("a", "alice", 300);
        addIsland("b", "bob", 200);
        addIsland("c", "carol", 100);
        addIsland("d", "dave", 50);
        addIsland("e", "eve", 25);

        // Characterizes the current (buggy) behaviour: toIndex = min(size - offset, length).
        // With size=5, offset=1, length=10 -> subList(1, min(4, 10)=4) yields 3 entries,
        // where a correct paging impl (subList(1, min(size, offset+length))) would return 4.
        List<IslandLevel> page = islandLogic.getRanks(1, 10);
        assertEquals(3, page.size());
        assertEquals("b", page.get(0).getIslandName());
        assertEquals("d", page.get(2).getIslandName());

        // ...and when offset is large enough that (size - offset) < length, fromIndex > toIndex,
        // so subList throws instead of returning a short page: subList(3, min(2, 1)=1).
        assertThrows(IllegalArgumentException.class, () -> islandLogic.getRanks(3, 1));
    }
}
