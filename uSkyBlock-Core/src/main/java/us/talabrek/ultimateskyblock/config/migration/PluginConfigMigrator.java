package us.talabrek.ultimateskyblock.config.migration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.imports.ItemComponentConverter;
import us.talabrek.ultimateskyblock.util.BackupFileUtil;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

@Singleton
public class PluginConfigMigrator {
    public static final int LEGACY_BASELINE_VERSION = 111;
    private static final String LEGACY_BASELINE_RESOURCE = "legacy/config-111.yml";
    private static final DateTimeFormatter BACKUP_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Logger logger;
    private final ConfigMigrations migrations;

    @Inject
    public PluginConfigMigrator(@NotNull Logger logger) {
        this(logger, ConfigMigrations.defaults());
    }

    private PluginConfigMigrator(@NotNull Logger logger, @NotNull ConfigMigrations migrations) {
        this.logger = logger;
        this.migrations = migrations;
    }

    public void migrate(@NotNull Path configPath, int currentVersion) {
        YamlConfiguration config = loadFromDisk(configPath);
        int version = config.getInt("version", 0);

        if (version < LEGACY_BASELINE_VERSION) {
            migrateWithLegacySystem(configPath);
            config = loadFromDisk(configPath);
            version = config.getInt("version", 0);
        }

        if (version < currentVersion) {
            applyExplicitMigrations(configPath, config, version, currentVersion);
        }
    }

    private void migrateWithLegacySystem(@NotNull Path configPath) {
        logger.info("Migrating config.yml using the legacy compatibility path.");
        new ItemComponentConverter(logger).checkAndDoConfigImport(requireParent(configPath).toFile());
        YamlConfiguration config = loadFromDisk(configPath);
        YamlConfiguration baselineConfig = loadBundledConfig(LEGACY_BASELINE_RESOURCE);
        migrateToLegacyBaseline(configPath, config, baselineConfig);
    }

    private void applyExplicitMigrations(@NotNull Path configPath, @NotNull YamlConfiguration config,
                                         int version, int currentVersion) {
        backupConfig(configPath, "before explicit migration");
        int nextVersion = version;
        while (nextVersion < currentVersion) {
            ConfigMigration migration = migrations.find(nextVersion);
            logger.info("Applying explicit config.yml migration " + migration.fromVersion() + " -> " + migration.toVersion() + ".");
            migration.apply(config);
            config.set("version", migration.toVersion());
            saveUnchecked(configPath, config, migration.fromVersion(), migration.toVersion());
            nextVersion = migration.toVersion();
        }
    }

    private void migrateToLegacyBaseline(@NotNull Path configPath, @NotNull YamlConfiguration config,
                                         @NotNull YamlConfiguration baselineConfig) {
        int currentVersion = config.getInt("version", 0);
        int baselineVersion = baselineConfig.getInt("version", LEGACY_BASELINE_VERSION);
        if (currentVersion >= baselineVersion) {
            return;
        }
        backupConfig(configPath, "before legacy migration");
        YamlConfiguration merged = mergeConfig(baselineConfig, config);
        saveUnchecked(configPath, merged, currentVersion, baselineVersion);
    }

    private void backupConfig(@NotNull Path configPath, @NotNull String context) {
        try {
            BackupFileUtil.copyToBackup(requireParent(configPath), configPath, backupFileName());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to back up config.yml " + context + ".", e);
        }
    }

    @NotNull
    private static String backupFileName() {
        return "config-" + LocalDateTime.now().format(BACKUP_TIMESTAMP_FORMAT) + ".yml";
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
    private YamlConfiguration loadFromDisk(@NotNull Path configPath) {
        YamlConfiguration config = new YamlConfiguration();
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            config.load(reader);
            return config;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load config.yml from " + configPath, e);
        }
    }

    private void saveUnchecked(@NotNull Path configPath, @NotNull YamlConfiguration config, int fromVersion, int toVersion) {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile(requireParent(configPath), "config-migration-", ".yml");
            config.save(tempFile.toFile());
            moveIntoPlace(tempFile, configPath);
        } catch (IOException e) {
            throw new IllegalStateException(
                "Unable to save config.yml after explicit migration " + fromVersion + " -> " + toVersion + ".", e);
        } finally {
            deleteIfExists(tempFile);
        }
    }

    private void moveIntoPlace(@NotNull Path source, @NotNull Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void deleteIfExists(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // Best effort cleanup only.
        }
    }

    @NotNull
    private static Path requireParent(@NotNull Path file) {
        Path parent = file.getParent();
        if (parent == null) {
            throw new IllegalStateException("Config file has no parent directory: " + file);
        }
        return parent;
    }
}
