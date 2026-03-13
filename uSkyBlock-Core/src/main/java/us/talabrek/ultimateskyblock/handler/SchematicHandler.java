package us.talabrek.ultimateskyblock.handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.config.PluginConfig;
import us.talabrek.ultimateskyblock.bootstrap.PluginDataDir;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Singleton
public class SchematicHandler {
    private static final String DEFAULT_SCHEME_NAME = "default";
    private static final String SCHEMATIC_PATH_KEY = "schematic";
    private static final String NETHER_SCHEMATIC_PATH_KEY = "nether-schematic";

    private final Logger logger;
    private final PluginConfig config;
    private final Path directorySchematics;

    private boolean initialized = false;

    @Inject
    public SchematicHandler(
        @NotNull Logger logger,
        @NotNull PluginConfig config,
        @NotNull @PluginDataDir Path dataFolder
    ) {
        this.logger = logger;
        this.config = config;
        this.directorySchematics = dataFolder.toAbsolutePath().resolve("schematics").normalize();
    }

    /**
     * Prepare schematic files: ensure directory, copy bundled files, and validate configured scheme paths.
     */
    public void initialize(@Nullable Plugin plugin) {
        if (initialized) {
            return;
        }
        try {
            Files.createDirectories(directorySchematics);
            copySchematicsFromJar(plugin);
            validateConfiguredSchemes();
            List<String> schemeNames = getSchemeNames();
            logger.info(schemeNames.isEmpty() ? "No island schemes configured." : schemeNames.size() + " island schemes configured.");
            initialized = true;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to prepare schematics", e);
        }
    }

    /**
     * Returns a list of configured, enabled scheme names.
     */
    public List<String> getSchemeNames() {
        ConfigurationSection islandSchemes = getIslandSchemes();
        if (islandSchemes == null) {
            return Collections.emptyList();
        }

        boolean netherEnabled = isNetherEnabled();
        List<String> names = new ArrayList<>();
        for (String schemeName : islandSchemes.getKeys(false)) {
            ConfigurationSection scheme = islandSchemes.getConfigurationSection(schemeName);
            if (scheme == null || !scheme.getBoolean("enabled", true)) {
                continue;
            }
            if (!hasConfiguredPath(scheme, SCHEMATIC_PATH_KEY)) {
                continue;
            }
            if (netherEnabled && !hasConfiguredPath(scheme, NETHER_SCHEMATIC_PATH_KEY)) {
                continue;
            }
            if (initialized && resolveConfiguredSchematicPath(scheme.getString(SCHEMATIC_PATH_KEY)).isEmpty()) {
                continue;
            }
            if (initialized && netherEnabled
                && resolveConfiguredSchematicPath(scheme.getString(NETHER_SCHEMATIC_PATH_KEY)).isEmpty()) {
                continue;
            }
            names.add(schemeName);
        }
        Collections.sort(names);
        return names;
    }

    /**
     * Resolve overworld/nether files for the given scheme name (or default when null).
     */
    @Nullable
    public SchematicPair getScheme(@Nullable String scheme) {
        String schemeName = scheme != null ? scheme : DEFAULT_SCHEME_NAME;
        ConfigurationSection schemeConfig = getIslandScheme(schemeName);
        if (schemeConfig == null || !schemeConfig.getBoolean("enabled", true)) {
            return null;
        }

        Optional<Path> overworld = resolveConfiguredSchematicPath(schemeConfig.getString(SCHEMATIC_PATH_KEY));
        if (overworld.isEmpty()) {
            return null;
        }

        if (!isNetherEnabled()) {
            return new SchematicPair(overworld.get(), Optional.empty());
        }

        Optional<Path> nether = resolveConfiguredSchematicPath(schemeConfig.getString(NETHER_SCHEMATIC_PATH_KEY));
        if (nether.isEmpty()) {
            return null;
        }
        return new SchematicPair(overworld.get(), nether);
    }

    /**
     * Returns the spawn schematic if present and readable.
     */
    public @Nullable Path getSpawnSchematic() {
        Path spawn = directorySchematics.resolve("spawn.schem");
        return Files.isRegularFile(spawn) && Files.isReadable(spawn) ? spawn : null;
    }

    /**
     * Copy all schematics in the plugins JAR-file to the plugins data folder.
     */
    private void copySchematicsFromJar(@Nullable Plugin plugin) {
        Class<?> source = plugin != null ? plugin.getClass() : getClass();
        CodeSource codeSource = source.getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            return;
        }

        URL jar = codeSource.getLocation();
        ClassLoader loader = source.getClassLoader();
        try (ZipInputStream zin = new ZipInputStream(jar.openStream())) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String prefix = "schematics/";
                if (!entry.getName().startsWith(prefix)) {
                    continue;
                }
                Optional<Path> targetPath = resolveTarget(directorySchematics, entry.getName().substring(prefix.length()));
                if (targetPath.isEmpty()) {
                    logger.warning("Skipping suspicious path while copying schematics: " + entry.getName());
                    continue;
                }
                Path target = targetPath.get();
                if (Files.exists(target)) {
                    continue;
                }
                try (InputStream inputStream = loader.getResourceAsStream(entry.getName())) {
                    if (inputStream != null) {
                        Files.createDirectories(target.getParent());
                        Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Unable to load schematic " + entry.getName(), e);
                }
                zin.closeEntry();
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to find schematics in plugin JAR", e);
        }
    }

    /**
     * Resolve a schematic target path relative to the schematics root, ensuring no path traversal.
     */
    static Optional<Path> resolveTarget(@NotNull Path schematicsRoot, @NotNull String entryRelativePath) {
        Path root = schematicsRoot.toAbsolutePath().normalize();
        Path target = root.resolve(entryRelativePath).normalize();
        if (!target.startsWith(root)) {
            return Optional.empty();
        }
        return Optional.of(target);
    }

    private void validateConfiguredSchemes() {
        ConfigurationSection islandSchemes = getIslandSchemes();
        if (islandSchemes == null) {
            return;
        }

        boolean netherEnabled = isNetherEnabled();
        for (String schemeName : islandSchemes.getKeys(false)) {
            ConfigurationSection scheme = islandSchemes.getConfigurationSection(schemeName);
            if (scheme == null || !scheme.getBoolean("enabled", true)) {
                continue;
            }
            validateConfiguredPath(schemeName, SCHEMATIC_PATH_KEY, scheme.getString(SCHEMATIC_PATH_KEY));
            if (netherEnabled) {
                validateConfiguredPath(schemeName, NETHER_SCHEMATIC_PATH_KEY, scheme.getString(NETHER_SCHEMATIC_PATH_KEY));
            }
        }
    }

    private void validateConfiguredPath(@NotNull String schemeName, @NotNull String key, @Nullable String configuredPath) {
        if (configuredPath == null || configuredPath.trim().isEmpty()) {
            logger.warning("Island scheme '" + schemeName + "' is missing required '" + key + "' configuration.");
            return;
        }

        Optional<Path> resolved = resolveTarget(directorySchematics, configuredPath);
        if (resolved.isEmpty()) {
            logger.warning("Island scheme '" + schemeName + "' has suspicious '" + key + "' path: " + configuredPath);
            return;
        }

        Path path = resolved.get();
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            logger.warning("Island scheme '" + schemeName + "' points '" + key + "' to a missing or unreadable file: " + configuredPath);
        }
    }

    private boolean isNetherEnabled() {
        return config.getYamlConfig().getBoolean("nether.enabled", false);
    }

    private boolean hasConfiguredPath(@NotNull ConfigurationSection scheme, @NotNull String key) {
        String configuredPath = scheme.getString(key);
        return configuredPath != null && !configuredPath.trim().isEmpty();
    }

    private @Nullable ConfigurationSection getIslandSchemes() {
        return config.getYamlConfig().getConfigurationSection("island-schemes");
    }

    private @Nullable ConfigurationSection getIslandScheme(@NotNull String schemeName) {
        ConfigurationSection islandSchemes = getIslandSchemes();
        return islandSchemes != null ? islandSchemes.getConfigurationSection(schemeName) : null;
    }

    private Optional<Path> resolveConfiguredSchematicPath(@Nullable String configuredPath) {
        if (configuredPath == null || configuredPath.trim().isEmpty()) {
            return Optional.empty();
        }

        return resolveTarget(directorySchematics, configuredPath)
            .filter(Files::isRegularFile)
            .filter(Files::isReadable);
    }

    public record SchematicPair(@NotNull Path overworld, @NotNull Optional<Path> nether) {
    }
}
