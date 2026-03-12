package us.talabrek.ultimateskyblock.config;

import dk.lockfuglsang.minecraft.file.FileUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.config.migration.PluginConfigMigrator;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Dedicated loader for config.yml. It owns config file IO and delegates all migration behavior
 * to {@link PluginConfigMigrator}.
 */
public class PluginConfigLoader {
    public static final String CONFIG_NAME = "config.yml";
    private static final Set<String> INVALID_SCHEMATIC_NAMES = Set.of("yourschematicname", "uSkyBlockDefault");

    private final PluginConfigMigrator migrator;

    public PluginConfigLoader(@NotNull Logger logger) {
        this(new PluginConfigMigrator(logger));
    }

    PluginConfigLoader(@NotNull PluginConfigMigrator migrator) {
        this.migrator = migrator;
    }

    @NotNull
    public YamlConfiguration load() {
        File configFile = FileUtil.getConfigFile(CONFIG_NAME);
        ensureConfigExists(configFile);

        int currentVersion = loadBundledConfig().getInt("version", PluginConfigMigrator.LEGACY_BASELINE_VERSION);
        YamlConfiguration config = loadFromDisk(configFile);
        int version = config.getInt("version", 0);

        if (version > currentVersion) {
            throw new IllegalStateException("config.yml version " + version
                + " is newer than the supported version " + currentVersion + ".");
        }

        if (version < currentVersion) {
            migrator.migrate(configFile, currentVersion);
            return loadFromDisk(configFile);
        }

        return config;
    }

    public void save(@NotNull YamlConfiguration config) throws IOException {
        config.save(FileUtil.getConfigFile(CONFIG_NAME));
    }

    @NotNull
    public static String normalizeIslandSchematicName(String schematicName) {
        if (schematicName == null || INVALID_SCHEMATIC_NAMES.contains(schematicName)) {
            return "default";
        }
        return schematicName;
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
            throw new IllegalStateException("Unable to load bundled config resource " + CONFIG_NAME, e);
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
