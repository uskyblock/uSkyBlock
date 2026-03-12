package us.talabrek.ultimateskyblock.config.migration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;

public final class ConfigMigrationV112 implements ConfigMigration {
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
        config.set("options.island.schematicName",
            PluginConfigLoader.normalizeIslandSchematicName(config.getString("options.island.schematicName", "default")));
    }
}
