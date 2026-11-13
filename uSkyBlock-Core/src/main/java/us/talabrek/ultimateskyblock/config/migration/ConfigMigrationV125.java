package us.talabrek.ultimateskyblock.config.migration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * v125: happy ghasts get their own spawn limit instead of counting as animals.
 */
public final class ConfigMigrationV125 implements ConfigMigration {
    private static final String HAPPY_GHASTS = "options.island.spawn-limits.happy-ghasts";

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
        if (!config.contains(HAPPY_GHASTS)) {
            config.set(HAPPY_GHASTS, 4);
        }
    }
}
