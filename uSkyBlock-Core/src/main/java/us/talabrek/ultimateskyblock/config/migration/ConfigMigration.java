package us.talabrek.ultimateskyblock.config.migration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public interface ConfigMigration {
    int fromVersion();

    int toVersion();

    void apply(@NotNull YamlConfiguration config);
}
