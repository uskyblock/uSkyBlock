package us.talabrek.ultimateskyblock.config.migration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class ConfigMigrationV122 implements ConfigMigration {
    private static final String TELEPORT_CANCEL_DISTANCE = "options.island.teleportCancelDistance";
    private static final String SIGNS_ENABLED = "signs.enabled";

    @Override
    public int fromVersion() {
        return 121;
    }

    @Override
    public int toVersion() {
        return 122;
    }

    @Override
    public void apply(@NotNull YamlConfiguration config, @NotNull Path pluginDataDir) {
        if (!config.contains(TELEPORT_CANCEL_DISTANCE)) {
            config.set(TELEPORT_CANCEL_DISTANCE, 0.5d);
        }
        if (!config.contains(SIGNS_ENABLED)) {
            config.set(SIGNS_ENABLED, true);
        }
    }
}
