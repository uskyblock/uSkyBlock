package us.talabrek.ultimateskyblock.config.migration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class ConfigMigrationV125 implements ConfigMigration {
    @Override
    public int fromVersion() {
        return 124;
    }

    @Override
    public int toVersion() {
        return 125;
    }

    @Override
    public void apply(@NotNull YamlConfiguration config, @NotNull Path pluginDataDir) {
        if (!config.contains("options.challenges.enabled")) {
            config.set("options.challenges.enabled", true);
        }
        if (!config.contains("options.challenges.reset-on-create")) {
            config.set("options.challenges.reset-on-create", true);
        }
        if (!config.contains("options.challenges.enable-economy-rewards")) {
            config.set("options.challenges.enable-economy-rewards", true);
        }
        if (!config.contains("options.challenges.broadcast.enabled")) {
            config.set("options.challenges.broadcast.enabled", true);
        }
        if (!config.contains("options.challenges.broadcast.prefix")) {
            config.set("options.challenges.broadcast.prefix", "");
        }
    }
}
