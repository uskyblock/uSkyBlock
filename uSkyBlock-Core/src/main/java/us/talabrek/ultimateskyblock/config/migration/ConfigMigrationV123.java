package us.talabrek.ultimateskyblock.config.migration;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

public final class ConfigMigrationV123 implements ConfigMigration {
    private static final String ISLAND_SCHEMES = "island-schemes";
    private static final String TOOL_MENU_COMMANDS = "tool-menu.commands";
    private static final String LEGACY_WORKBENCH = "WORKBENCH";
    private static final String CRAFTING_TABLE = "CRAFTING_TABLE";

    @Override
    public int fromVersion() {
        return 122;
    }

    @Override
    public int toVersion() {
        return 123;
    }

    @Override
    public void apply(@NotNull YamlConfiguration config, @NotNull Path pluginDataDir) {
        normalizeSchemeExtraItems(config);
        migrateWorkbenchToolMenuCommand(config);
    }

    private void normalizeSchemeExtraItems(@NotNull YamlConfiguration config) {
        ConfigurationSection schemes = config.getConfigurationSection(ISLAND_SCHEMES);
        if (schemes == null) {
            return;
        }
        for (String schemeName : schemes.getKeys(false)) {
            String extraItemsPath = ISLAND_SCHEMES + "." + schemeName + ".extraItems";
            Object value = config.get(extraItemsPath);
            if (value instanceof String stringValue && stringValue.isBlank()) {
                config.set(extraItemsPath, List.of());
            }
        }
    }

    private void migrateWorkbenchToolMenuCommand(@NotNull YamlConfiguration config) {
        String legacyPath = TOOL_MENU_COMMANDS + "." + LEGACY_WORKBENCH;
        if (!config.contains(legacyPath)) {
            return;
        }
        String command = config.getString(legacyPath, "");
        String canonicalPath = TOOL_MENU_COMMANDS + "." + CRAFTING_TABLE;
        if (!config.contains(canonicalPath)) {
            config.set(canonicalPath, command);
        }
        config.set(legacyPath, null);
    }
}
