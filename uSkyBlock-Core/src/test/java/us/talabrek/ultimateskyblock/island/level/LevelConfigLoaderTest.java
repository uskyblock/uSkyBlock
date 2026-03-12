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
    public void movesExistingDataFolderLevelConfigIntoBackupBeforeLoadingBundledConfig() throws Exception {
        Path legacyLevelConfig = tempDir.resolve(LevelConfigLoader.LEVEL_CONFIG_NAME);
        Files.writeString(legacyLevelConfig, "general:\n  pointsPerLevel: 999\n");

        LevelConfigLoader loader = new LevelConfigLoader(tempDir, Logger.getAnonymousLogger());
        YamlConfiguration config = loader.load();

        assertEquals(1000, config.getInt("general.pointsPerLevel"));
        assertFalse(Files.exists(legacyLevelConfig));
        Path backupPath = tempDir.resolve("backup").resolve(LevelConfigLoader.LEVEL_CONFIG_NAME);
        assertTrue(Files.exists(backupPath));
        assertEquals("general:\n  pointsPerLevel: 999\n", Files.readString(backupPath));
    }

    @Test
    public void usesSuffixedBackupNameWhenBackupAlreadyExists() throws Exception {
        Path backupDir = tempDir.resolve("backup");
        Files.createDirectories(backupDir);
        Files.writeString(backupDir.resolve(LevelConfigLoader.LEVEL_CONFIG_NAME), "existing backup");
        Files.writeString(tempDir.resolve(LevelConfigLoader.LEVEL_CONFIG_NAME), "legacy copy");

        LevelConfigLoader loader = new LevelConfigLoader(tempDir, Logger.getAnonymousLogger());
        loader.load();

        assertTrue(Files.exists(backupDir.resolve("levelConfig-1.yml")));
        assertEquals("legacy copy", Files.readString(backupDir.resolve("levelConfig-1.yml")));
    }
}
