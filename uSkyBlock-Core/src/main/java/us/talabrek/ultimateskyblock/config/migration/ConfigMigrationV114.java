package us.talabrek.ultimateskyblock.config.migration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class ConfigMigrationV114 implements ConfigMigration {
    @Override
    public int fromVersion() {
        return 113;
    }

    @Override
    public int toVersion() {
        return 114;
    }

    @Override
    public void apply(@NotNull YamlConfiguration config, @NotNull Path pluginDataDir) {
        config.set("force-replace", null);
        config.set("move-nodes", null);
        config.set("options.deprecated.fixFlatland", null);

        if (config.isConfigurationSection("options.deprecated")
            && config.getConfigurationSection("options.deprecated") != null
            && config.getConfigurationSection("options.deprecated").getKeys(false).isEmpty()) {
            config.set("options.deprecated", null);
        }
    }
}
