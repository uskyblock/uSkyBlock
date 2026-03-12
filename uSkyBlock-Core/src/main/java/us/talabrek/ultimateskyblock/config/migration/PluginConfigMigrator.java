package us.talabrek.ultimateskyblock.config.migration;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.imports.ItemComponentConverter;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

public class PluginConfigMigrator {
    public static final int LEGACY_BASELINE_VERSION = 111;
    private static final String LEGACY_BASELINE_RESOURCE = "legacy/config-111.yml";

    private final Logger logger;
    private final ConfigMigrations migrations;

    public PluginConfigMigrator(@NotNull Logger logger) {
        this(logger, ConfigMigrations.defaults());
    }

    PluginConfigMigrator(@NotNull Logger logger, @NotNull ConfigMigrations migrations) {
        this.logger = logger;
        this.migrations = migrations;
    }

    public void migrate(@NotNull File configFile, int currentVersion) {
        YamlConfiguration config = loadFromDisk(configFile);
        int version = config.getInt("version", 0);

        if (version < LEGACY_BASELINE_VERSION) {
            migrateWithLegacySystem(configFile);
            config = loadFromDisk(configFile);
            version = config.getInt("version", 0);
        }

        if (version < currentVersion) {
            applyExplicitMigrations(configFile, config, version, currentVersion);
        }
    }

    private void migrateWithLegacySystem(@NotNull File configFile) {
        logger.info("Migrating config.yml using the legacy compatibility path.");
        new ItemComponentConverter(logger).checkAndDoConfigImport(requireParent(configFile));
        YamlConfiguration config = loadFromDisk(configFile);
        YamlConfiguration baselineConfig = loadBundledConfig(LEGACY_BASELINE_RESOURCE);
        migrateToLegacyBaseline(configFile, config, baselineConfig);
    }

    private void applyExplicitMigrations(@NotNull File configFile, @NotNull YamlConfiguration config,
                                         int version, int currentVersion) {
        int nextVersion = version;
        while (nextVersion < currentVersion) {
            ConfigMigration migration = migrations.find(nextVersion);
            logger.info("Applying explicit config.yml migration " + migration.fromVersion() + " -> " + migration.toVersion() + ".");
            migration.apply(config);
            config.set("version", migration.toVersion());
            saveUnchecked(configFile, config, migration.fromVersion(), migration.toVersion());
            nextVersion = migration.toVersion();
        }
    }

    private void migrateToLegacyBaseline(@NotNull File configFile, @NotNull YamlConfiguration config,
                                         @NotNull YamlConfiguration baselineConfig) {
        int currentVersion = config.getInt("version", 0);
        int baselineVersion = baselineConfig.getInt("version", LEGACY_BASELINE_VERSION);
        if (currentVersion >= baselineVersion) {
            return;
        }
        backupForLegacyMigration(configFile);
        YamlConfiguration merged = mergeConfig(baselineConfig, config);
        saveUnchecked(configFile, merged, currentVersion, baselineVersion);
    }

    private void backupForLegacyMigration(@NotNull File configFile) {
        try {
            File backupFolder = new File(requireParent(configFile), "backup");
            backupFolder.mkdirs();
            String backupName = String.format("config-%1$tY%1$tm%1$td-%1$tH%1$tM.yml", new Date());
            Files.move(Paths.get(configFile.toURI()),
                Paths.get(new File(backupFolder, backupName).toURI()),
                StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to back up config.yml before legacy migration.", e);
        }
    }

    @NotNull
    private YamlConfiguration mergeConfig(@NotNull YamlConfiguration source, @NotNull YamlConfiguration destination) {
        int existing = destination.getInt("version");
        int version = source.getInt("version", existing);
        destination.setDefaults(source);
        destination.options().copyDefaults(true);
        destination.set("version", version);
        removeExcludes(destination);
        moveNodes(source, destination);
        replaceDefaults(source, destination);
        return destination;
    }

    private void removeExcludes(@NotNull YamlConfiguration destination) {
        List<String> keys = destination.getStringList("merge-ignore");
        for (String key : keys) {
            requireNonNull(destination.getDefaults()).set(key, null);
        }
    }

    private void replaceDefaults(@NotNull YamlConfiguration source, @NotNull YamlConfiguration destination) {
        ConfigurationSection forceSection = source.getConfigurationSection("force-replace");
        if (forceSection != null) {
            for (String key : forceSection.getKeys(true)) {
                Object oldDefault = forceSection.get(key, null);
                Object value = destination.get(key, oldDefault);
                Object newDefault = source.get(key, null);
                if (oldDefault != null && oldDefault.equals(value)) {
                    destination.set(key, newDefault);
                }
            }
        }
        destination.set("force-replace", null);
        requireNonNull(destination.getDefaults()).set("force-replace", null);
    }

    private void moveNodes(@NotNull YamlConfiguration source, @NotNull YamlConfiguration destination) {
        ConfigurationSection moveSection = source.getConfigurationSection("move-nodes");
        if (moveSection != null) {
            List<String> keys = new ArrayList<>(moveSection.getKeys(true));
            Collections.reverse(keys);
            for (String key : keys) {
                if (moveSection.isString(key)) {
                    String sourcePath = key;
                    String targetPath = moveSection.getString(key, key);
                    Object value = destination.get(sourcePath);
                    if (value != null) {
                        destination.set(targetPath, value);
                        destination.set(sourcePath, null);
                    }
                } else if (moveSection.isConfigurationSection(key)) {
                    if (destination.isConfigurationSection(key)
                        && requireNonNull(destination.getConfigurationSection(key)).getKeys(false).isEmpty()) {
                        destination.set(key, null);
                    }
                }
            }
        }
        destination.set("move-nodes", null);
        requireNonNull(destination.getDefaults()).set("move-nodes", null);
    }

    @NotNull
    private YamlConfiguration loadBundledConfig(@NotNull String resourceName) {
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(
            getClass().getClassLoader().getResourceAsStream(resourceName)), StandardCharsets.UTF_8)) {
            YamlConfiguration config = new YamlConfiguration();
            config.load(reader);
            return config;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load bundled config resource " + resourceName, e);
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

    private void saveUnchecked(@NotNull File configFile, @NotNull YamlConfiguration config, int fromVersion, int toVersion) {
        try {
            config.save(configFile);
        } catch (IOException e) {
            throw new IllegalStateException(
                "Unable to save config.yml after explicit migration " + fromVersion + " -> " + toVersion + ".", e);
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
