package us.talabrek.ultimateskyblock;

import dk.lockfuglsang.minecraft.file.FileUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.imports.ItemComponentConverter;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Dedicated loader for config.yml with a strict cut-over between the old implicit migration path
 * and future explicit migrations.
 */
public class PluginConfigLoader {
    static final String CONFIG_NAME = "config.yml";
    static final int LEGACY_BASELINE_VERSION = 111;
    private static final List<String> REQUIRED_CURRENT_PATHS = List.of(
        "language",
        "options.general.maxPartySize",
        "options.general.worldName",
        "options.general.spawnSize",
        "options.island.height",
        "options.island.schematicName",
        "options.advanced.manageSpawn",
        "options.restart.teleportDelay",
        "nether.enabled"
    );

    private final Logger logger;

    public PluginConfigLoader(@NotNull Logger logger) {
        this.logger = logger;
    }

    @NotNull
    public YamlConfiguration load() {
        File configFile = FileUtil.getConfigFile(CONFIG_NAME);
        ensureConfigExists(configFile);

        YamlConfiguration bundledConfig = loadBundledConfig();
        int currentVersion = bundledConfig.getInt("version", LEGACY_BASELINE_VERSION);

        YamlConfiguration config = loadFromDisk(configFile);
        int version = config.getInt("version", 0);

        if (version > currentVersion) {
            throw new IllegalStateException("config.yml version " + version
                + " is newer than the supported version " + currentVersion + ".");
        }

        if (version < LEGACY_BASELINE_VERSION) {
            migrateWithLegacySystem(configFile);
            config = loadFromDisk(configFile);
            verifyCurrentLayout(config, LEGACY_BASELINE_VERSION, "Legacy migration");
            version = config.getInt("version", 0);
        }

        if (version < currentVersion) {
            throw new IllegalStateException("config.yml version " + version + " requires explicit migrations up to "
                + currentVersion + ", but no new migration steps are registered yet.");
        }

        verifyCurrentLayout(config, currentVersion, "Config validation");

        return config;
    }

    public void save(@NotNull YamlConfiguration config) throws IOException {
        config.save(FileUtil.getConfigFile(CONFIG_NAME));
    }

    private void migrateWithLegacySystem(@NotNull File configFile) {
        logger.info("Migrating config.yml using the legacy compatibility path.");
        new ItemComponentConverter(logger).checkAndDoConfigImport(requireParent(configFile));
        // Legacy merge path is still the authoritative upgrader for versions below the cut-over.
        FileUtil.getYmlConfiguration(CONFIG_NAME);
    }

    private void verifyCurrentLayout(@NotNull YamlConfiguration config, int expectedVersion, @NotNull String phase) {
        int version = config.getInt("version", 0);
        if (version != expectedVersion) {
            throw new IllegalStateException(phase + " did not produce the expected config.yml version "
                + expectedVersion + ". Found version " + version + " instead.");
        }
        for (String path : REQUIRED_CURRENT_PATHS) {
            if (!config.contains(path)) {
                throw new IllegalStateException(phase + " produced an invalid config.yml. Missing required path: " + path);
            }
        }
    }

    private void ensureConfigExists(@NotNull File configFile) {
        if (configFile.exists()) {
            return;
        }
        File parent = requireParent(configFile);
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Unable to create config directory " + parent);
        }
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(
            getClass().getClassLoader().getResourceAsStream(CONFIG_NAME)), StandardCharsets.UTF_8)) {
            YamlConfiguration config = new YamlConfiguration();
            config.load(reader);
            config.save(configFile);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create initial config.yml", e);
        }
    }

    @NotNull
    private YamlConfiguration loadBundledConfig() {
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(
            getClass().getClassLoader().getResourceAsStream(CONFIG_NAME)), StandardCharsets.UTF_8)) {
            YamlConfiguration config = new YamlConfiguration();
            config.load(reader);
            return config;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load bundled config.yml", e);
        }
    }

    @NotNull
    private YamlConfiguration loadFromDisk(@NotNull File configFile) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(configFile);
            return config;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load config.yml from " + configFile, e);
        }
    }

    @NotNull
    private static File requireParent(@NotNull File file) {
        File parent = file.getParentFile();
        if (parent == null) {
            throw new IllegalStateException("Config file has no parent directory: " + file);
        }
        return parent;
    }
}
