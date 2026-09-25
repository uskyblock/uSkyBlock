package us.talabrek.ultimateskyblock.island.level;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Focused tests for {@link BlockCountCollection#add(Material, int)} and its single-argument overload.
 * <p>
 * {@code calculateScore(...)} and {@link CommonLevelLogic#createIslandScore} are intentionally not covered here:
 * both construct {@link BlockScoreImpl} instances, whose constructor resolves the block name through
 * {@code ItemStackUtil.getBlockName -> Material.getBlockTranslationKey() -> Registry.BLOCK}. That registry is only
 * populated by a live server, so it throws under {@link BukkitServerMock}. Those paths belong in the live-server
 * harness, not a unit test.
 */
public class BlockCountCollectionTest {

    private BlockLevelConfigMap configMap;

    @BeforeEach
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        BukkitServerMock.setupServerMock();

        // Default config: linear score, no limits/returns. Only the grouping behaviour is under test here.
        BlockLevelConfigBuilder defaultBuilder = new BlockLevelConfigBuilder().scorePerBlock(10);
        List<BlockLevelConfig> collection = new ArrayList<>();
        collection.add(defaultBuilder.copy().base(Material.STONE).build());
        collection.add(defaultBuilder.copy().base(Material.DIRT).build());
        // OAK_LOG is an additional block of the OAK_WOOD config, so both map to the OAK_WOOD key.
        collection.add(defaultBuilder.copy().base(Material.OAK_WOOD)
                .additionalBlocks(new BlockMatch(Material.OAK_LOG))
                .build());
        configMap = new BlockLevelConfigMap(collection, defaultBuilder);
    }

    @Test
    public void addReturnsCumulativeCountForBlockType() {
        BlockCountCollection counts = new BlockCountCollection(configMap);

        assertEquals(10, counts.add(Material.STONE, 10));
        assertEquals(15, counts.add(Material.STONE, 5));
    }

    @Test
    public void additionalBlocksAccumulateUnderBaseBlockKey() {
        BlockCountCollection counts = new BlockCountCollection(configMap);

        // OAK_WOOD (base) and OAK_LOG (additional block) share the same BlockMatch key, so counts are summed.
        assertEquals(3, counts.add(Material.OAK_WOOD, 3));
        assertEquals(5, counts.add(Material.OAK_LOG, 2));
    }

    @Test
    public void distinctBlockTypesAccumulateIndependently() {
        BlockCountCollection counts = new BlockCountCollection(configMap);

        assertEquals(10, counts.add(Material.STONE, 10));
        assertEquals(4, counts.add(Material.DIRT, 4));
        assertEquals(11, counts.add(Material.STONE, 1));
        assertEquals(6, counts.add(Material.DIRT, 2));
    }

    @Test
    public void singleArgumentAddIncrementsByOne() {
        BlockCountCollection counts = new BlockCountCollection(configMap);

        assertEquals(1, counts.add(Material.STONE));
        assertEquals(2, counts.add(Material.STONE));
    }

    @Test
    public void varietyCountsKeepMaterialsSeparateEvenWhenLegacyConfigGroupsThem() {
        BlockCountCollection legacy = new BlockCountCollection(configMap);
        BlockCountCollection variety = BlockCountCollection.forVariety();
        assertEquals(1000, legacy.add(Material.OAK_WOOD, 1000));
        assertEquals(1001, legacy.add(Material.OAK_LOG, 1));
        assertEquals(1000, variety.add(Material.OAK_WOOD, 1000));
        assertEquals(1, variety.add(Material.OAK_LOG, 1));

        assertEquals(1000, variety.materialCounts().get(Material.OAK_WOOD));
        assertEquals(1, variety.materialCounts().get(Material.OAK_LOG));
        VarietyLevelPolicy policy = new VarietyLevelPolicy(1);
        double separate = policy.levelFor(Material.OAK_WOOD, 1000) + policy.levelFor(Material.OAK_LOG, 1);
        assertTrue(separate > policy.levelFor(Material.OAK_WOOD, 1001));
        assertEquals(2, variety.materialCounts().size());
    }

    @Test
    public void varietyCollectionDoesNotRequireBlockValueConfiguration() {
        BlockCountCollection counts = BlockCountCollection.forVariety();
        assertEquals(5, counts.add(Material.GLASS, 5));
        assertEquals(1, counts.add(Material.DIAMOND_BLOCK));
        assertEquals(5, counts.materialCounts().get(Material.GLASS));
        assertEquals(1, counts.materialCounts().get(Material.DIAMOND_BLOCK));
        assertThrows(IllegalStateException.class, () -> counts.calculateScore(1000));
    }
}
