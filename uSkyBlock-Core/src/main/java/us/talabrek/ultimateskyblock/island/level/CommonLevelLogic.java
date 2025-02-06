package us.talabrek.ultimateskyblock.island.level;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import us.talabrek.ultimateskyblock.PluginConfig;
import us.talabrek.ultimateskyblock.Settings;
import us.talabrek.ultimateskyblock.api.model.BlockScore;
import us.talabrek.ultimateskyblock.island.level.yml.LevelConfigYmlReader;
import us.talabrek.ultimateskyblock.world.WorldManager;

import java.util.List;

public abstract class CommonLevelLogic implements LevelLogic {
    FileConfiguration levelConfig;
    private final WorldManager worldManager;

    BlockLevelConfigMap scoreMap;
    private final int pointsPerLevel;
    final int activateNetherAtLevel;

    CommonLevelLogic(FileConfiguration levelConfig, WorldManager worldManager) {
        this.levelConfig = levelConfig;
        activateNetherAtLevel = levelConfig.getInt("nether.activate-at.level", 100);
        pointsPerLevel = levelConfig.getInt("general.pointsPerLevel");
        this.worldManager = worldManager;
        load();
    }

    private void load() {
        scoreMap = new LevelConfigYmlReader().readLevelConfig(levelConfig);
    }

    Location getNetherLocation(Location location) {
        Location netherLocation = location.clone();
        netherLocation.setWorld(worldManager.getNetherWorld());
        netherLocation.setY(Settings.nether_height);
        return netherLocation;
    }

    IslandScore createIslandScore(BlockCountCollection blockCollection) {
        List<BlockScore> blockScores = blockCollection.calculateScore(pointsPerLevel);
        return new IslandScore(blockScores.stream().mapToDouble(BlockScore::getScore).sum(), blockScores);
    }
}
