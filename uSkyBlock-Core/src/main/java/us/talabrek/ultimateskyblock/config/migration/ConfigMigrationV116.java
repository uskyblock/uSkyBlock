package us.talabrek.ultimateskyblock.config.migration;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ConfigMigrationV116 implements ConfigMigration {
    private static final String SCHEMATICS_DIR_NAME = "schematics";
    private static final String DEFAULT_NETHER_SCHEMATIC = "uSkyBlockNether";
    private static final List<String> SCHEMATIC_EXTENSIONS = List.of(".schematic", ".schem");

    @Override
    public int fromVersion() {
        return 115;
    }

    @Override
    public int toVersion() {
        return 116;
    }

    @Override
    public void apply(@NotNull YamlConfiguration config, @NotNull Path pluginDataDir) {
        ConfigurationSection islandSchemes = config.getConfigurationSection("island-schemes");
        if (islandSchemes == null) {
            config.set("nether.schematicName", null);
            return;
        }

        Path schematicsDir = pluginDataDir.resolve(SCHEMATICS_DIR_NAME);
        String legacyNetherName = config.getString("nether.schematicName", DEFAULT_NETHER_SCHEMATIC);
        for (String schemeName : new ArrayList<>(islandSchemes.getKeys(false))) {
            if ("spawn".equals(schemeName)) {
                config.set("island-schemes." + schemeName, null);
                continue;
            }

            ConfigurationSection scheme = islandSchemes.getConfigurationSection(schemeName);
            if (scheme == null) {
                continue;
            }

            if (isBlank(scheme.getString("schematic"))) {
                scheme.set("schematic", resolveLegacyOverworldPath(schematicsDir, schemeName));
            }
            if (isBlank(scheme.getString("nether-schematic"))) {
                scheme.set("nether-schematic", resolveLegacyNetherPath(schematicsDir, schemeName, legacyNetherName));
            }
        }

        config.set("nether.schematicName", null);
    }

    @NotNull
    private static String resolveLegacyOverworldPath(@NotNull Path schematicsDir, @NotNull String schemeName) {
        return resolveLegacyPath(schematicsDir, schemeName)
            .orElseGet(() -> withDefaultExtension(schemeName, ".schematic"));
    }

    @NotNull
    private static String resolveLegacyNetherPath(@NotNull Path schematicsDir, @NotNull String schemeName,
                                                  @NotNull String legacyNetherName) {
        return resolveLegacyPath(schematicsDir, schemeName + "Nether")
            .or(() -> resolveLegacyPath(schematicsDir, legacyNetherName))
            .orElseGet(() -> withDefaultExtension(legacyNetherName,
                DEFAULT_NETHER_SCHEMATIC.equals(legacyNetherName) ? ".schem" : ".schematic"));
    }

    @NotNull
    private static Optional<String> resolveLegacyPath(@NotNull Path schematicsDir, @NotNull String baseNameOrPath) {
        String legacyName = normalizeLegacyName(baseNameOrPath);
        if (isBlank(legacyName)) {
            return Optional.empty();
        }

        Path asConfigured = schematicsDir.resolve(legacyName).normalize();
        if (Files.isRegularFile(asConfigured)) {
            return Optional.of(toRelativeString(schematicsDir, asConfigured));
        }

        for (String extension : SCHEMATIC_EXTENSIONS) {
            Path candidate = schematicsDir.resolve(legacyName + extension).normalize();
            if (Files.isRegularFile(candidate)) {
                return Optional.of(toRelativeString(schematicsDir, candidate));
            }
        }
        return Optional.empty();
    }

    @NotNull
    private static String withDefaultExtension(@NotNull String baseNameOrPath, @NotNull String defaultExtension) {
        String legacyName = normalizeLegacyName(baseNameOrPath);
        if (legacyName.endsWith(".schem") || legacyName.endsWith(".schematic")) {
            return legacyName;
        }
        return legacyName + defaultExtension;
    }

    @NotNull
    private static String toRelativeString(@NotNull Path schematicsDir, @NotNull Path resolvedPath) {
        return schematicsDir.toAbsolutePath().normalize()
            .relativize(resolvedPath.toAbsolutePath().normalize())
            .toString()
            .replace('\\', '/');
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @NotNull
    private static String normalizeLegacyName(@NotNull String baseNameOrPath) {
        String normalized = baseNameOrPath.replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        return lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
    }
}
