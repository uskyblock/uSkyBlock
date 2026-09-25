package us.talabrek.ultimateskyblock.island.level;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import us.talabrek.ultimateskyblock.island.level.yml.LevelConfigYmlReader;
import us.talabrek.ultimateskyblock.world.WorldManager;

public abstract class CommonLevelLogic implements LevelLogic {
    FileConfiguration levelConfig;
    private final WorldManager worldManager;
    private final int netherHeight;

    BlockLevelConfigMap scoreMap;
    private final IslandLevelCurve levelCurve;
    final int activateNetherAtLevel;

    CommonLevelLogic(FileConfiguration levelConfig, WorldManager worldManager, int netherHeight) {
        this.levelConfig = levelConfig;
        activateNetherAtLevel = levelConfig.getInt("nether.activate-at.level", 100);
        levelCurve = IslandLevelCurve.fromConfig(levelConfig);
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
        // Apply the curve once to the combined points, never once per material or dimension.
        return IslandScore.fromPoints(blockCollection.calculateScore(1), levelCurve);
    }
}
