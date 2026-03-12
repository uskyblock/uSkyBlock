package us.talabrek.ultimateskyblock.config;

import com.google.inject.Inject;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.bootstrap.PluginDataDir;
import us.talabrek.ultimateskyblock.config.migration.PluginConfigMigrator;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Dedicated loader for config.yml. It owns config file IO and delegates all migration behavior
 * to {@link PluginConfigMigrator}.
 */
public class PluginConfigLoader {
    public static final String CONFIG_NAME = "config.yml";
    private static final Set<String> INVALID_SCHEMATIC_NAMES = Set.of("yourschematicname", "uSkyBlockDefault");

    private final Path configPath;
    private final PluginConfigMigrator migrator;

    @Inject
    public PluginConfigLoader(@NotNull @PluginDataDir Path pluginDataDir, @NotNull PluginConfigMigrator migrator) {
        this.configPath = pluginDataDir.resolve(CONFIG_NAME);
        this.migrator = migrator;
    }

    @NotNull
    public YamlConfiguration load() {
        validateNoLocalizedConfigFiles();
        ensureConfigExists(configPath);

        int currentVersion = loadBundledConfig().getInt("version", PluginConfigMigrator.LEGACY_BASELINE_VERSION);
        YamlConfiguration config = loadFromDisk(configPath);
        int version = config.getInt("version", 0);

        if (version > currentVersion) {
            throw new IllegalStateException("config.yml version " + version
                + " is newer than the supported version " + currentVersion + ".");
        }

        if (version < currentVersion) {
            migrator.migrate(configPath, currentVersion);
            return loadFromDisk(configPath);
        }

        return config;
    }

    public void save(@NotNull YamlConfiguration config) throws IOException {
        validateNoLocalizedConfigFiles();
        config.save(configPath.toFile());
    }

    @NotNull
    public static String normalizeIslandSchematicName(String schematicName) {
        if (schematicName == null || INVALID_SCHEMATIC_NAMES.contains(schematicName)) {
            return "default";
        }
        return schematicName;
    }

    private void ensureConfigExists(@NotNull Path configPath) {
        if (Files.exists(configPath)) {
            return;
        }
        Path parent = requireParent(configPath);
        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create config directory " + parent);
        }
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(
            getClass().getClassLoader().getResourceAsStream(CONFIG_NAME)), StandardCharsets.UTF_8)) {
            YamlConfiguration config = new YamlConfiguration();
            config.load(reader);
            config.save(configPath.toFile());
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
    private YamlConfiguration loadFromDisk(@NotNull Path configPath) {
        YamlConfiguration config = new YamlConfiguration();
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            config.load(reader);
            return config;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load config.yml from " + configPath, e);
        }
    }

    @NotNull
    private void validateNoLocalizedConfigFiles() {
        if (Files.exists(configPath)) {
            return;
        }
        Path dataFolder = requireParent(configPath);
        if (!Files.exists(dataFolder)) {
            return;
        }
        try (Stream<Path> configFiles = Files.list(dataFolder)) {
            Path localizedConfig = configFiles
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().matches("config_.+\\.yml"))
                .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                .findFirst()
                .orElse(null);
            if (localizedConfig != null) {
                throw new IllegalStateException("Localized config file '" + localizedConfig.getFileName()
                    + "' is no longer supported. Rename it to '" + CONFIG_NAME + "'.");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to inspect config directory " + dataFolder, e);
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
