package us.talabrek.ultimateskyblock;

import dk.lockfuglsang.minecraft.file.FileUtil;
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
import java.util.Set;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

/**
 * Dedicated loader for config.yml with a strict cut-over between the old implicit migration path
 * and future explicit migrations.
 */
public class PluginConfigLoader {
    static final String CONFIG_NAME = "config.yml";
    static final int LEGACY_BASELINE_VERSION = 111;
    private static final String LEGACY_BASELINE_RESOURCE = "legacy/config-111.yml";
    private static final Set<String> INVALID_SCHEMATIC_NAMES = Set.of("yourschematicname", "uSkyBlockDefault");
    private static final List<String> REQUIRED_CURRENT_PATHS = List.of(
        "language",
        "options.general.maxPartySize",
        "options.general.worldName",
        "options.general.spawnSize",
        "options.island.height",
        "options.island.schematicName",
        "options.extras.obsidianToLava",
        "options.advanced.manageSpawn",
        "options.restart.teleportDelay",
        "nether.enabled"
    );
    private static final List<ConfigMigration> EXPLICIT_MIGRATIONS = List.of(
        new ConfigMigration(111, 112, config -> {
            if (!config.contains("options.extras.obsidianToLava")) {
                config.set("options.extras.obsidianToLava", true);
            }
            config.set("options.island.schematicName",
                normalizeIslandSchematicName(config.getString("options.island.schematicName", "default")));
        })
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
            config = applyExplicitMigrations(configFile, config, version, currentVersion);
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
        YamlConfiguration config = loadFromDisk(configFile);
        YamlConfiguration baselineConfig = loadBundledConfig(LEGACY_BASELINE_RESOURCE);
        migrateToLegacyBaseline(configFile, config, baselineConfig);
    }

    @NotNull
    private YamlConfiguration applyExplicitMigrations(@NotNull File configFile, @NotNull YamlConfiguration config,
                                                      int version, int currentVersion) {
        int nextVersion = version;
        while (nextVersion < currentVersion) {
            ConfigMigration migration = findMigration(nextVersion);
            logger.info("Applying explicit config.yml migration " + migration.fromVersion + " -> " + migration.toVersion + ".");
            migration.apply(config);
            config.set("version", migration.toVersion);
            saveUnchecked(configFile, config, migration.fromVersion, migration.toVersion);
            nextVersion = migration.toVersion;
        }
        return loadFromDisk(configFile);
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
        String schematicName = config.getString("options.island.schematicName");
        if (schematicName == null || !normalizeIslandSchematicName(schematicName).equals(schematicName)) {
            throw new IllegalStateException(phase + " produced an invalid config.yml. Invalid schematic name: " + schematicName);
        }
        if (!config.isBoolean("options.extras.obsidianToLava")) {
            throw new IllegalStateException(phase + " produced an invalid config.yml. options.extras.obsidianToLava must be boolean.");
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
        return loadBundledConfig(CONFIG_NAME);
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

    @NotNull
    private static File requireParent(@NotNull File file) {
        File parent = file.getParentFile();
        if (parent == null) {
            throw new IllegalStateException("Config file has no parent directory: " + file);
        }
        return parent;
    }

    @NotNull
    static String normalizeIslandSchematicName(String schematicName) {
        if (schematicName == null || INVALID_SCHEMATIC_NAMES.contains(schematicName)) {
            return "default";
        }
        return schematicName;
    }

    @NotNull
    private ConfigMigration findMigration(int fromVersion) {
        return EXPLICIT_MIGRATIONS.stream()
            .filter(migration -> migration.fromVersion == fromVersion)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "config.yml version " + fromVersion + " requires explicit migrations, but no step is registered."));
    }

    private void saveUnchecked(@NotNull File configFile, @NotNull YamlConfiguration config, int fromVersion, int toVersion) {
        try {
            config.save(configFile);
        } catch (IOException e) {
            throw new IllegalStateException(
                "Unable to save config.yml after explicit migration " + fromVersion + " -> " + toVersion + ".", e);
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

    @FunctionalInterface
    private interface MigrationAction {
        void apply(@NotNull YamlConfiguration config);
    }

    private static final class ConfigMigration {
        private final int fromVersion;
        private final int toVersion;
        private final MigrationAction action;

        private ConfigMigration(int fromVersion, int toVersion, @NotNull MigrationAction action) {
            this.fromVersion = fromVersion;
            this.toVersion = toVersion;
            this.action = action;
        }

        private void apply(@NotNull YamlConfiguration config) {
            action.apply(config);
        }
    }
}
