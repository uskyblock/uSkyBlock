package us.talabrek.ultimateskyblock.config.migration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class ConfigMigrationV112 implements ConfigMigration {
    private static final Set<String> LEGACY_SCHEMATIC_PLACEHOLDERS = Set.of(
        "yourschematicname",
        "yourschematichere",
        "uSkyBlockDefault"
    );

    @Override
    public int fromVersion() {
        return 111;
    }

    @Override
    public int toVersion() {
        return 112;
    }

    @Override
    public void apply(@NotNull YamlConfiguration config) {
        if (!config.contains("options.extras.obsidianToLava")) {
            config.set("options.extras.obsidianToLava", true);
        }
        config.set("options.island.schematicName", normalizeIslandSchematicName(
            config.getString("options.island.schematicName", "default")));
    }

    @NotNull
    private static String normalizeIslandSchematicName(String schematicName) {
        if (schematicName == null || LEGACY_SCHEMATIC_PLACEHOLDERS.contains(schematicName)) {
            return "default";
        }
        return schematicName;
    }
}
