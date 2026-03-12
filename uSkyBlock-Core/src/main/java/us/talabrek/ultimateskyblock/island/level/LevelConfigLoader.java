package us.talabrek.ultimateskyblock.island.level;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.bootstrap.PluginDataDir;
import us.talabrek.ultimateskyblock.util.BackupFileUtil;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Logger;

@Singleton
public class LevelConfigLoader {
    static final String LEVEL_CONFIG_NAME = "levelConfig.yml";

    private final Path pluginDataDir;
    private final Logger logger;

    @Inject
    public LevelConfigLoader(@NotNull @PluginDataDir Path pluginDataDir, @NotNull Logger logger) {
        this.pluginDataDir = pluginDataDir;
        this.logger = logger;
    }

    public @NotNull YamlConfiguration load() {
        moveLegacyDataFolderCopyToBackup();
        try (var stream = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(LEVEL_CONFIG_NAME),
            "Missing bundled resource " + LEVEL_CONFIG_NAME);
             var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load bundled " + LEVEL_CONFIG_NAME, e);
        }
    }

    private void moveLegacyDataFolderCopyToBackup() {
        Path levelConfigPath = pluginDataDir.resolve(LEVEL_CONFIG_NAME);
        if (!Files.exists(levelConfigPath)) {
            return;
        }
        try {
            Path backupPath = BackupFileUtil.moveToBackup(pluginDataDir, levelConfigPath, LEVEL_CONFIG_NAME);
            logger.info("Moved legacy " + LEVEL_CONFIG_NAME + " from the data folder to " + backupPath + ".");
        } catch (IOException e) {
            throw new IllegalStateException("Unable to back up legacy " + LEVEL_CONFIG_NAME + ".", e);
        }
    }
}
