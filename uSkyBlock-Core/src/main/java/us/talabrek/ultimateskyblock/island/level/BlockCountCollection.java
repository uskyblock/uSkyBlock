package us.talabrek.ultimateskyblock.island.level;

import org.bukkit.Material;
import us.talabrek.ultimateskyblock.api.model.BlockScore;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

/**
 * Mutable collection for storing counts of blocks
 */
public class BlockCountCollection {
    private final BlockLevelConfigMap configMap;
    private final Map<BlockMatch, LongAdder> countMap;
    private final Map<Material, LongAdder> materialCounts;

    public BlockCountCollection(BlockLevelConfigMap configMap) {
        this.configMap = Objects.requireNonNull(configMap);
        countMap = new ConcurrentHashMap<>();
        materialCounts = null;
    }

    private BlockCountCollection() {
        configMap = null;
        countMap = null;
        materialCounts = new EnumMap<>(Material.class);
    }

    public static BlockCountCollection forVariety() {
        return new BlockCountCollection();
    }

    public int add(Material type, int blockCount) {
        if (blockCount < 0) {
            throw new IllegalArgumentException("Block count must not be negative");
        }
        if (configMap == null) {
            LongAdder materialCount = materialCounts.computeIfAbsent(type, k -> new LongAdder());
            materialCount.add(blockCount);
            return materialCount.intValue();
        }
        BlockMatch key = configMap.get(type).getKey();
        LongAdder groupedCount = countMap.computeIfAbsent(key, k -> new LongAdder());
        groupedCount.add(blockCount);
        return groupedCount.intValue();
    }

    public int add(Material type) {
        return add(type, 1);
    }

    public List<BlockScore> calculateScore(double pointsPerLevel) {
        if (configMap == null) {
            throw new IllegalStateException("Legacy block scores are unavailable in variety mode");
        }
        return countMap.entrySet().stream()
                .map(e -> configMap.get(e.getKey()).calculateScore(e.getValue().intValue(), pointsPerLevel))
                .filter(f -> f.getScore() != 0)
                .sorted(new BlockScoreComparator()).collect(Collectors.toList());
    }

    public List<BlockScore> calculateVarietyScore(VarietyLevelPolicy policy) {
        if (materialCounts == null) {
            throw new IllegalStateException("Material counts are unavailable in legacy mode");
        }
        return materialCounts.entrySet().stream()
                .filter(entry -> policy.levelFor(entry.getKey(), entry.getValue().intValue()) > 0)
                .map(entry -> new BlockScoreImpl(entry.getKey().createBlockData(), entry.getValue().intValue(),
                        policy.levelFor(entry.getKey(), entry.getValue().intValue()),
                        entry.getValue().intValue() == 1 ? BlockScore.State.NORMAL : BlockScore.State.DIMINISHING))
                .sorted(new BlockScoreComparator()).collect(Collectors.toList());
    }

    Map<Material, Integer> materialCounts() {
        if (materialCounts == null) {
            throw new IllegalStateException("Material counts are unavailable in legacy mode");
        }
        Map<Material, Integer> snapshot = new EnumMap<>(Material.class);
        materialCounts.forEach((material, count) -> snapshot.put(material, count.intValue()));
        return Collections.unmodifiableMap(snapshot);
    }
}
