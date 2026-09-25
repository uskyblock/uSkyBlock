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
    final int activateNetherAtLevel;

    CommonLevelLogic(FileConfiguration levelConfig, WorldManager worldManager, int netherHeight) {
        this.levelConfig = levelConfig;
        // TODO 4.0: Either make this an explicit levelConfig.yml key again or hardcode/remove the threshold entirely.
        // It does not belong in config.yml; it controls when nether score starts counting toward island level.
        activateNetherAtLevel = levelConfig.getInt("nether.activate-at.level", 100);
        pointsPerLevel = levelConfig.getInt("general.pointsPerLevel");
        this.worldManager = worldManager;
        this.netherHeight = netherHeight;
        load();
    }

    private void load() {
        scoreMap = new LevelConfigYmlReader().readLevelConfig(levelConfig);
    }

    Location getNetherLocation(Location location) {
        Location netherLocation = location.clone();
        netherLocation.setWorld(worldManager.getNetherWorld());
        netherLocation.setY(netherHeight);
        return netherLocation;
    }

    IslandScore createIslandScore(BlockCountCollection blockCollection) {
        List<BlockScore> blockScores = blockCollection.calculateScore(pointsPerLevel);
        return new IslandScore(blockScores.stream().mapToDouble(BlockScore::getScore).sum(), blockScores);
    }
}
