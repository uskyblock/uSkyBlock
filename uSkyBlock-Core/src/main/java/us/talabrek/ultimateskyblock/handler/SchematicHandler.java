package us.talabrek.ultimateskyblock.handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.bootstrap.PluginDataDir;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;

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
    private final RuntimeConfigs runtimeConfigs;
    private final Path directorySchematics;

    private boolean initialized = false;

    @Inject
    public SchematicHandler(
        @NotNull Logger logger,
        @NotNull RuntimeConfigs runtimeConfigs,
        @NotNull @PluginDataDir Path dataFolder
    ) {
        this.logger = logger;
        this.runtimeConfigs = runtimeConfigs;
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
            validateDefaultScheme();
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
        RuntimeConfig runtimeConfig = runtimeConfigs.current();
        if (runtimeConfig.islandSchemes().isEmpty()) {
            return Collections.emptyList();
        }

        boolean netherEnabled = runtimeConfig.nether().enabled();
        List<String> names = new ArrayList<>();
        for (var entry : runtimeConfig.islandSchemes().entrySet()) {
            String schemeName = entry.getKey();
            RuntimeConfig.IslandScheme scheme = entry.getValue();
            if (!scheme.enabled()) {
                continue;
            }
            if (!hasConfiguredPath(scheme.schematic())) {
                continue;
            }
            if (netherEnabled && !hasConfiguredPath(scheme.netherSchematic())) {
                continue;
            }
            if (initialized && resolveConfiguredSchematicPath(scheme.schematic()).isEmpty()) {
                continue;
            }
            if (initialized && netherEnabled
                && resolveConfiguredSchematicPath(scheme.netherSchematic()).isEmpty()) {
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
        RuntimeConfig runtimeConfig = runtimeConfigs.current();
        RuntimeConfig.IslandScheme schemeConfig = runtimeConfig.islandScheme(schemeName);
        if (schemeConfig == null || !schemeConfig.enabled()) {
            return null;
        }

        Optional<Path> overworld = resolveConfiguredSchematicPath(schemeConfig.schematic());
        if (overworld.isEmpty()) {
            return null;
        }

        if (!runtimeConfig.nether().enabled()) {
            return new SchematicPair(overworld.get(), Optional.empty());
        }

        Optional<Path> nether = resolveConfiguredSchematicPath(schemeConfig.netherSchematic());
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
        RuntimeConfig runtimeConfig = runtimeConfigs.current();
        boolean netherEnabled = runtimeConfig.nether().enabled();
        for (var entry : runtimeConfig.islandSchemes().entrySet()) {
            String schemeName = entry.getKey();
            RuntimeConfig.IslandScheme scheme = entry.getValue();
            if (!scheme.enabled()) {
                continue;
            }
            validateConfiguredPath(schemeName, SCHEMATIC_PATH_KEY, scheme.schematic());
            if (netherEnabled) {
                validateConfiguredPath(schemeName, NETHER_SCHEMATIC_PATH_KEY, scheme.netherSchematic());
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

    private void validateDefaultScheme() {
        RuntimeConfig runtimeConfig = runtimeConfigs.current();
        String defaultScheme = runtimeConfig.island().defaultScheme();
        if (defaultScheme == null || defaultScheme.isBlank()) {
            logger.warning("Configured default island scheme is missing.");
            return;
        }

        RuntimeConfig.IslandScheme scheme = runtimeConfig.islandScheme(defaultScheme);
        if (scheme == null) {
            logger.warning("Configured default island scheme '" + defaultScheme + "' does not exist.");
            return;
        }
        if (!scheme.enabled()) {
            logger.warning("Configured default island scheme '" + defaultScheme + "' is disabled.");
            return;
        }
        if (getScheme(defaultScheme) == null) {
            logger.warning("Configured default island scheme '" + defaultScheme + "' is not usable.");
        }
    }

    private boolean hasConfiguredPath(@Nullable String configuredPath) {
        return configuredPath != null && !configuredPath.trim().isEmpty();
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
