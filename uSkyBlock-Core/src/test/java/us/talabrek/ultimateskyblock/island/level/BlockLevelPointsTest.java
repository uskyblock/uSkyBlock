package us.talabrek.ultimateskyblock.island.level;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.api.model.BlockScore;

import static org.junit.jupiter.api.Assertions.*;

class BlockLevelPointsTest {
    @Test
    void respectsHardLimitsAndNegativeReturns() {
        BlockLevelConfig capped = block().limit(100).build();
        assertEquals(500, capped.calculatePoints(50).points());
        assertEquals(1000, capped.calculatePoints(1000).points());
        assertEquals(BlockScore.State.LIMIT, capped.calculatePoints(1000).state());
        BlockLevelConfig penalty = block().negativeReturns(100).build();
        assertEquals(0, penalty.calculatePoints(200).points());
        assertEquals(-1000, penalty.calculatePoints(300).points());
        assertEquals(BlockScore.State.NEGATIVE, penalty.calculatePoints(300).state());
        assertThrows(IllegalArgumentException.class, () -> capped.calculatePoints(-1));
    }

    @Test
    void diversityBeatsTheSameNumberOfRepeatedBlocksWithDiminishingReturns() {
        BlockLevelConfig scored = block().diminishingReturns(100).build();
        assertEquals(1000, scored.calculatePoints(100).points());
        assertEquals(2000, scored.calculatePoints(300).points(), 1e-9);
        assertEquals(BlockScore.State.DIMINISHING, scored.calculatePoints(300).state());

        double singleTypePoints = scored.calculatePoints(1000).points();
        double tenTypesPoints = 10 * scored.calculatePoints(100).points();
        assertTrue(tenTypesPoints > singleTypePoints);
        IslandLevelCurve curve = new IslandLevelCurve(10, 100, 1.5);
        assertTrue(curve.levelForPoints(tenTypesPoints) > curve.levelForPoints(singleTypePoints));
    }

    @Test
    void diminishingReturnsAreContinuousAndNeverMakeAnotherBlockReduceTheScore() {
        BlockLevelConfig scored = block().diminishingReturns(100).build();
        double previous = 0;
        for (int count : new int[]{0, 99, 100, 101, 200, 1000, 10_000, 1_000_000}) {
            double points = scored.calculatePoints(count).points();
            assertTrue(points >= previous);
            previous = points;
        }
        assertTrue(scored.calculatePoints(101).points() < 1010);
    }

    private static BlockLevelConfigBuilder block() {
        return new BlockLevelConfigBuilder().base(Material.STONE).scorePerBlock(10);
    }
}
