package us.talabrek.ultimateskyblock.island.level;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.bootstrap.PluginDataDir;
import us.talabrek.ultimateskyblock.bootstrap.PluginLog;

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
    public LevelConfigLoader(@NotNull @PluginDataDir Path pluginDataDir, @NotNull @PluginLog Logger logger) {
        this.pluginDataDir = pluginDataDir;
        this.logger = logger;
    }

    public @NotNull YamlConfiguration load() {
        Path levelConfigPath = pluginDataDir.resolve(LEVEL_CONFIG_NAME);
        if (Files.exists(levelConfigPath)) {
            logger.info("Loading " + LEVEL_CONFIG_NAME + " from " + levelConfigPath + ".");
            return YamlConfiguration.loadConfiguration(levelConfigPath.toFile());
        }

        try (var stream = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(LEVEL_CONFIG_NAME),
            "Missing bundled resource " + LEVEL_CONFIG_NAME);
             var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            logger.info("Loading bundled " + LEVEL_CONFIG_NAME + ".");
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load " + LEVEL_CONFIG_NAME, e);
        }
    }
}
