package us.talabrek.ultimateskyblock.island.level;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import us.talabrek.ultimateskyblock.api.model.BlockScore;
import us.talabrek.ultimateskyblock.island.level.yml.LevelConfigYmlReader;
import us.talabrek.ultimateskyblock.world.WorldManager;

import java.util.List;

public abstract class CommonLevelLogic implements LevelLogic {
    FileConfiguration levelConfig;
    private final WorldManager worldManager;
    private final int netherHeight;

    BlockLevelConfigMap scoreMap;
    private final int pointsPerLevel;
    private final VarietyLevelPolicy varietyPolicy;
    final int activateNetherAtLevel;

    CommonLevelLogic(FileConfiguration levelConfig, WorldManager worldManager, int netherHeight) {
        this.levelConfig = levelConfig;
        // TODO 4.0: Either make this an explicit levelConfig.yml key again or hardcode/remove the threshold entirely.
        // It does not belong in config.yml; it controls when nether score starts counting toward island level.
        activateNetherAtLevel = levelConfig.getInt("nether.activate-at.level", 100);
        String scoringMode = levelConfig.getString("general.scoringMode", "legacy");
        if ("variety".equalsIgnoreCase(scoringMode)) {
            varietyPolicy = VarietyLevelPolicy.fromConfig(levelConfig);
            pointsPerLevel = 0;
        } else if ("legacy".equalsIgnoreCase(scoringMode)) {
            varietyPolicy = null;
            pointsPerLevel = levelConfig.getInt("general.pointsPerLevel");
        } else {
            throw new IllegalArgumentException("general.scoringMode must be legacy or variety");
        }
        this.worldManager = worldManager;
        this.netherHeight = netherHeight;
        load();
    }

    private void load() {
        if (varietyPolicy == null) {
            scoreMap = new LevelConfigYmlReader().readLevelConfig(levelConfig);
        }
    }

    Location getNetherLocation(Location location) {
        Location netherLocation = location.clone();
        netherLocation.setWorld(worldManager.getNetherWorld());
        netherLocation.setY(netherHeight);
        return netherLocation;
    }

    BlockCountCollection newBlockCounts() {
        return varietyPolicy == null ? new BlockCountCollection(scoreMap) : BlockCountCollection.forVariety();
    }

    IslandScore createIslandScore(BlockCountCollection blockCollection) {
        if (varietyPolicy != null) {
            List<BlockScore> blockScores = blockCollection.calculateVarietyScore(varietyPolicy);
            return new IslandScore(blockScores.stream().mapToDouble(BlockScore::getScore).sum(), blockScores, false);
        }
        List<BlockScore> blockScores = blockCollection.calculateScore(pointsPerLevel);
        return new IslandScore(blockScores.stream().mapToDouble(BlockScore::getScore).sum(), blockScores);
    }
}
