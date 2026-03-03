package us.talabrek.ultimateskyblock.island.level;

import us.talabrek.ultimateskyblock.api.model.BlockScore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static us.talabrek.ultimateskyblock.message.Msg.plainText;

/**
 * The summary of island calculation.
 */
public class IslandScore implements us.talabrek.ultimateskyblock.api.model.IslandScore {
    private final double score;
    private final List<BlockScore> top;
    private boolean isSorted = false;

    public IslandScore(double score, List<BlockScore> top) {
        this.score = score;
        this.top = joinTop(top);
    }

    /**
     * Consolidates the top, so scores with the same name is combined.
     */
    private List<BlockScore> joinTop(List<BlockScore> top) {
        Map<String, BlockScore> scoreMap = new HashMap<>();
        for (BlockScore score : top) {
            String key = plainText(score.getComponentName());
            BlockScore existing = scoreMap.get(key);
            if (existing == null) {
                scoreMap.put(key, score);
            } else {
                scoreMap.put(key, add(score, existing));
            }
        }
        return new ArrayList<>(scoreMap.values());
    }

    private BlockScoreImpl add(BlockScore score, BlockScore existing) {
        BlockScore.State state = score.getState();
        if (score.getState().ordinal() > existing.getState().ordinal()) {
            state = existing.getState();
        }
        return new BlockScoreImpl(existing.getBlockData(),
                score.getCount() + existing.getCount(),
                score.getScore() + existing.getScore(), state, score.getComponentName());
    }

    @Override
    public double getScore() {
        return score;
    }

    public List<BlockScore> getTop() {
        return top;
    }

    @Override
    public List<BlockScore> getTop(int num) {
        return getTop(0, num);
    }

    @Override
    public List<BlockScore> getTop(int offset, int num) {
        if (num <= 0) {
            throw new IllegalArgumentException("Number must be a positive integer.");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be a non-negative integer.");
        }
        if (!isSorted) {
            top.sort(new BlockScoreComparator());
            isSorted = true;
        }
        return top.subList(offset, Math.min(offset+num, top.size()));
    }

    @Override
    public int getSize() {
        return top.size();
    }
}
