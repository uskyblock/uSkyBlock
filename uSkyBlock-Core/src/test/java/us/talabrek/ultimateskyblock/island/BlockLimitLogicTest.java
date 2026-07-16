package us.talabrek.ultimateskyblock.island;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigFactory;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.island.BlockLimitLogic.CanPlace;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BlockLimitLogic}: config parsing of the block-limit map, the
 * {@link CanPlace} decision vs. a configured limit, inc/dec bookkeeping, and the
 * -1 (unknown) / -2 (uncertain) sentinels returned by {@code getCount}.
 *
 * <p>The block-limit map is supplied through a real {@link RuntimeConfig} (records cannot be
 * mocked). Note that the bundled config.yml always ships two default block limits
 * (HOPPER=50, SPAWNER=10), so those are present in addition to any limits configured here.
 */
public class BlockLimitLogicTest {

    private Logger logger;

    @BeforeEach
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        BukkitServerMock.setupServerMock();
        logger = mock(Logger.class);
    }

    /**
     * Builds a BlockLimitLogic whose only relevant config input is the block-limit map,
     * reached via runtimeConfigs.current().island().blockLimits(). The map is loaded through
     * a real RuntimeConfig; the factory uses a separate real logger so that only
     * BlockLimitLogic's own parse warnings land on the verified mock {@code logger}.
     */
    private BlockLimitLogic buildLogic(Map<String, Integer> configuredLimits) {
        YamlConfiguration config = baseConfig();
        configuredLimits.forEach((key, limit) -> config.set("options.island.block-limits." + key, limit));
        RuntimeConfig runtimeConfig = new RuntimeConfigFactory(new GameObjectFactory(), Logger.getAnonymousLogger())
            .load(config);
        RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
        doReturn(runtimeConfig).when(runtimeConfigs).current();
        return new BlockLimitLogic(runtimeConfigs, logger);
    }

    /**
     * Minimal config that the RuntimeConfigFactory can fully load (mirrors InternalEventsTest).
     */
    private YamlConfiguration baseConfig() {
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
        return config;
    }

    private Map<String, Integer> single(String key, int limit) {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put(key, limit);
        return map;
    }

    @Test
    public void parsesValidMaterialsAndIgnoresBogusEntries() {
        Map<String, Integer> configured = new LinkedHashMap<>();
        configured.put("STONE", 5);              // valid, kept
        configured.put("DIRT", 0);               // valid (limit 0 >= 0), kept
        configured.put("NOT_A_REAL_BLOCK", 3);   // unknown material, ignored
        configured.put("GLASS", -1);             // valid material but negative limit, ignored

        BlockLimitLogic logic = buildLogic(configured);

        // Valid custom entries are kept; bogus ones are dropped. Exact map size is not asserted
        // because the bundled config.yml always contributes HOPPER/SPAWNER defaults on top.
        assertTrue(logic.getLimits().containsKey(Material.STONE));
        assertTrue(logic.getLimits().containsKey(Material.DIRT));
        assertFalse(logic.getLimits().containsKey(Material.GLASS));

        assertEquals(5, logic.getLimit(Material.STONE));
        assertEquals(0, logic.getLimit(Material.DIRT));
        // Unconfigured / rejected materials fall back to the "no limit" sentinel.
        assertEquals(Integer.MAX_VALUE, logic.getLimit(Material.GLASS));

        // One warning for the unknown material, one for the negative limit (defaults are valid).
        verify(logger).warning(contains("NOT_A_REAL_BLOCK"));
        verify(logger).warning(contains("GLASS"));
        verify(logger, times(2)).warning(anyString());
    }

    @Test
    public void getCountReturnsUnknownAndUncertainSentinels() {
        BlockLimitLogic logic = buildLogic(single("STONE", 5));
        Location island = new Location(null, 0, 0, 0);

        // Material not under a limit -> -1 (unknown).
        assertEquals(-1, logic.getCount(Material.GLASS, island));
        // Limited material but no count recorded for this island yet -> -2 (uncertain).
        assertEquals(-2, logic.getCount(Material.STONE, island));
    }

    @Test
    public void incAndDecTrackCountsPerIslandAndMaterial() {
        BlockLimitLogic logic = buildLogic(single("STONE", 5));
        Location island = new Location(null, 100, 64, 100);

        logic.incBlockCount(island, Material.STONE);
        assertEquals(1, logic.getCount(Material.STONE, island));

        logic.incBlockCount(island, Material.STONE);
        assertEquals(2, logic.getCount(Material.STONE, island));

        logic.decBlockCount(island, Material.STONE);
        assertEquals(1, logic.getCount(Material.STONE, island));

        // Materials without a limit are ignored by the bookkeeping and stay "unknown".
        logic.incBlockCount(island, Material.GLASS);
        assertEquals(-1, logic.getCount(Material.GLASS, island));
    }

    @Test
    public void canPlaceReflectsCountVersusLimit() {
        BlockLimitLogic logic = buildLogic(single("STONE", 2));
        Location loc = new Location(null, 0, 0, 0);
        IslandInfo islandInfo = mock(IslandInfo.class);
        when(islandInfo.getIslandLocation()).thenReturn(loc);

        // No count recorded yet -> uncertain.
        assertEquals(CanPlace.UNCERTAIN, logic.canPlace(Material.STONE, islandInfo));

        logic.incBlockCount(loc, Material.STONE); // count 1 < limit 2
        assertEquals(CanPlace.YES, logic.canPlace(Material.STONE, islandInfo));

        logic.incBlockCount(loc, Material.STONE); // count 2, not < limit 2
        assertEquals(CanPlace.NO, logic.canPlace(Material.STONE, islandInfo));

        // A material that is not limited can always be placed.
        assertEquals(CanPlace.YES, logic.canPlace(Material.GLASS, islandInfo));
    }

    @Test
    public void unlistedMaterialsAreUnlimited() {
        // With no custom limits, only the bundled defaults (HOPPER/SPAWNER) are enforced, so an
        // unlisted material like STONE has no limit. (An entirely empty block-limit map is
        // unreachable through a real config load, since config.yml always ships HOPPER/SPAWNER;
        // the original getLimits().isEmpty() assertion is therefore dropped.)
        BlockLimitLogic logic = buildLogic(Map.of());
        Location loc = new Location(null, 0, 0, 0);
        IslandInfo islandInfo = mock(IslandInfo.class);
        when(islandInfo.getIslandLocation()).thenReturn(loc);

        assertEquals(-1, logic.getCount(Material.STONE, loc));
        assertEquals(CanPlace.YES, logic.canPlace(Material.STONE, islandInfo));
        // The default HOPPER/SPAWNER limits are valid, so nothing is logged.
        verifyNoInteractions(logger);
    }
}
