package us.talabrek.ultimateskyblock.island.level;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LevelConfigLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    public void loadsBundledLevelConfigWithoutCreatingDataFolderCopy() {
        LevelConfigLoader loader = new LevelConfigLoader(tempDir, Logger.getAnonymousLogger());

        YamlConfiguration config = loader.load();

        assertEquals(1000, config.getInt("general.pointsPerLevel"));
        assertFalse(Files.exists(tempDir.resolve(LevelConfigLoader.LEVEL_CONFIG_NAME)));
    }

    @Test
    public void loadsExistingDataFolderLevelConfig() throws Exception {
        Path levelConfig = tempDir.resolve(LevelConfigLoader.LEVEL_CONFIG_NAME);
        Files.writeString(levelConfig, "general:\n  pointsPerLevel: 999\n");

        LevelConfigLoader loader = new LevelConfigLoader(tempDir, Logger.getAnonymousLogger());
        YamlConfiguration config = loader.load();

        assertEquals(999, config.getInt("general.pointsPerLevel"));
        assertEquals("general:\n  pointsPerLevel: 999\n", Files.readString(levelConfig));
    }

    @Test
    public void keepsExistingDataFolderLevelConfigUnchangedAcrossLoads() throws Exception {
        Path levelConfig = tempDir.resolve(LevelConfigLoader.LEVEL_CONFIG_NAME);
        Files.writeString(levelConfig, "general:\n  pointsPerLevel: 777\n");

        LevelConfigLoader loader = new LevelConfigLoader(tempDir, Logger.getAnonymousLogger());
        YamlConfiguration firstLoad = loader.load();
        YamlConfiguration secondLoad = loader.load();

        assertEquals(777, firstLoad.getInt("general.pointsPerLevel"));
        assertEquals(777, secondLoad.getInt("general.pointsPerLevel"));
        assertEquals("general:\n  pointsPerLevel: 777\n", Files.readString(levelConfig));
    }
}
