package us.talabrek.ultimateskyblock.config.migration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public interface ConfigMigration {
    int fromVersion();

    int toVersion();

    void apply(@NotNull YamlConfiguration config, @NotNull Path pluginDataDir);
}
