package us.talabrek.ultimateskyblock.island.level;

import net.kyori.adventure.text.Component;
import org.bukkit.block.data.BlockData;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.api.model.BlockScore;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class IslandScoreCurveTest {
    @Test
    void appliesCurveOnceToTotalAndKeepsBreakdownInLevelUnits() {
        BlockScore stone = points("Stone", 100_000, BlockScore.State.DIMINISHING);
        BlockScore glass = points("Glass", 300_000, BlockScore.State.LIMIT);
        IslandScore score = IslandScore.fromPoints(List.of(stone, glass), new IslandLevelCurve(1000, 100, 2));

        assertEquals(200, score.getScore());
        List<BlockScore> top = score.getTop(2);
        assertEquals(150, top.get(0).getScore());
        assertEquals(50, top.get(1).getScore());
        assertEquals(score.getScore(), top.stream().mapToDouble(BlockScore::getScore).sum());
        assertEquals(BlockScore.State.LIMIT, top.get(0).getState());
        assertEquals(glass.getBlockData(), top.get(0).getBlockData());
        assertEquals(glass.getCount(), top.get(0).getCount());
        assertEquals(300_000, glass.getScore(), "conversion must not mutate its input");
    }

    @Test
    void zeroAndNegativeTotalsDoNotCreateNaNOrReversePenalties() {
        IslandLevelCurve curve = new IslandLevelCurve(1000, 100, 2);
        assertEquals(0, IslandScore.fromPoints(List.of(), curve).getScore());
        IslandScore zero = IslandScore.fromPoints(List.of(
            points("Stone", 1000, BlockScore.State.NORMAL),
            points("Penalty", -1000, BlockScore.State.NEGATIVE)), curve);
        assertEquals(0, zero.getScore());
        assertEquals(List.of(1d, -1d), zero.getTop(2).stream().map(BlockScore::getScore).toList());
        IslandScore negative = IslandScore.fromPoints(List.of(points("Penalty", -2000, BlockScore.State.NEGATIVE)), curve);
        assertEquals(-2, negative.getScore());
        assertEquals(-2, negative.getTop(1).getFirst().getScore());
    }

    @Test
    void defaultCurvePreservesLegacyBlockContributions() {
        IslandScore score = IslandScore.fromPoints(List.of(
            points("Stone", 100_000, BlockScore.State.NORMAL),
            points("Glass", 300_000, BlockScore.State.NORMAL)), new IslandLevelCurve(1000, 100, 1));
        assertEquals(400, score.getScore());
        assertEquals(List.of(300d, 100d), score.getTop(2).stream().map(BlockScore::getScore).toList());
    }

    private static BlockScore points(String name, double points, BlockScore.State state) {
        return new BlockScoreImpl(mock(BlockData.class), 10, points, state, Component.text(name));
    }
}
