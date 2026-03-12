package us.talabrek.ultimateskyblock.handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.config.PluginConfig;
import us.talabrek.ultimateskyblock.bootstrap.PluginDataDir;
import us.talabrek.ultimateskyblock.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Singleton
public class SchematicHandler {

    // Current state: mirrors the legacy IslandGenerator behavior (copy bundled schematics, scan data/schematics
    // with island-schemes.<name>.enabled gating, and resolve overworld/nether by naming convention). This keeps
    // functionality 1:1 for now; the whole schematics subsystem would benefit from a dedicated redesign to separate
    // discovery, layout, and pasting for clarity and robustness.
    private final Logger logger;
    private final PluginConfig config;
    private final Path directorySchematics;
    private final String defaultNetherName;

    private List<Path> schematicFiles = Collections.emptyList();
    private @Nullable Path netherSchematic;
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
        this.defaultNetherName = config.getYamlConfig().getString("nether.schematicName", "uSkyBlockNether");
    }

    /**
     * Prepare schematic files: ensure directory, copy bundled files, and scan the folder.
     */
    public void initialize(@Nullable Plugin plugin) {
        if (initialized) {
            return;
        }
        try {
            if (!Files.exists(directorySchematics)) {
                Files.createDirectories(directorySchematics);
            }
            copySchematicsFromJar(plugin);
            netherSchematic = getSchematicFile(defaultNetherName);
            schematicFiles = loadSchematics();
            logger.info(schematicFiles.isEmpty() ? "No schematic file loaded." : schematicFiles.size() + " schematics loaded.");
            initialized = true;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to prepare schematics", e);
        }
    }

    /**
     * Returns a list of available schematic names (basename).
     */
    public List<String> getSchemeNames() {
        List<String> names = new ArrayList<>();
        for (Path p : schematicFiles) {
            names.add(FileUtil.getBasename(p.getFileName().toString()));
        }
        Collections.sort(names);
        return names;
    }

    /**
     * Resolve overworld/nether files for the given scheme name (or default when null).
     */
    @Nullable
    public SchematicPair getScheme(@Nullable String scheme) {
        String schemeName = scheme != null ? scheme : "default";
        Path overworld = getSchematicFile(schemeName);
        if (overworld == null) {
            return null;
        }

        Path nether = getSchematicFile(scheme != null ? scheme + "Nether" : defaultNetherName);
        if (nether == null) {
            nether = netherSchematic;
        }
        return new SchematicPair(overworld, Optional.ofNullable(nether));
    }

    private List<Path> loadSchematics() throws IOException {
        if (!Files.isDirectory(directorySchematics)) {
            return Collections.emptyList();
        }
        List<Path> result = new ArrayList<>();
        try (Stream<Path> paths = Files.list(directorySchematics)) {
            paths.filter(Files::isRegularFile)
                .filter(p -> isIslandSchematic(p.getFileName().toString()))
                .forEach(result::add);
        }
        return result;
    }

    private boolean isIslandSchematic(String name) {
        String basename = FileUtil.getBasename(name);
        boolean enabled = config.getYamlConfig().getBoolean("island-schemes." + basename + ".enabled", true);
        return enabled
            && name != null
            && (name.endsWith(".schematic") || name.endsWith(".schem"))
            && !name.startsWith("uSkyBlock")
            && !basename.toLowerCase(Locale.ROOT).endsWith("nether");
    }

    private Path getSchematicFile(String cSchem) {
        List<String> extensions = List.of("schematic", "schem");
        for (String ext : extensions) {
            Path candidate = directorySchematics.resolve(cSchem + "." + ext);
            if (Files.exists(candidate) && Files.isRegularFile(candidate) && Files.isReadable(candidate)) {
                return candidate;
            }
        }
        return null;
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
                File targetFile = target.toFile();
                if (targetFile.exists()) {
                    continue;
                }
                try (InputStream inputStream = loader.getResourceAsStream(entry.getName())) {
                    if (inputStream != null) {
                        Files.createDirectories(target.getParent());
                        FileUtil.copy(inputStream, targetFile);
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

    public record SchematicPair(@NotNull Path overworld, @NotNull Optional<Path> nether) {
    }
}
