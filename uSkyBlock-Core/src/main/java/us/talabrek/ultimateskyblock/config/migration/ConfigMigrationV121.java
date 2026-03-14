package us.talabrek.ultimateskyblock.config.migration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class ConfigMigrationV121 implements ConfigMigration {
    private static final String LEGACY_DEFAULT_SCHEME = "options.island.schematicName";
    private static final String CANONICAL_DEFAULT_SCHEME = "options.island.default-scheme";
    private static final String LEGACY_NETHER_LAVA_LEVEL = "nether.lava_level";
    private static final String CANONICAL_NETHER_LAVA_LEVEL = "nether.lava-level";

    @Override
    public int fromVersion() {
        return 120;
    }

    @Override
    public int toVersion() {
        return 121;
    }

    @Override
    public void apply(@NotNull YamlConfiguration config, @NotNull Path pluginDataDir) {
        if (config.contains(LEGACY_DEFAULT_SCHEME) && !config.contains(CANONICAL_DEFAULT_SCHEME)) {
            config.set(CANONICAL_DEFAULT_SCHEME, config.get(LEGACY_DEFAULT_SCHEME));
        }
        config.set(LEGACY_DEFAULT_SCHEME, null);

        if (config.contains(LEGACY_NETHER_LAVA_LEVEL) && !config.contains(CANONICAL_NETHER_LAVA_LEVEL)) {
            config.set(CANONICAL_NETHER_LAVA_LEVEL, config.get(LEGACY_NETHER_LAVA_LEVEL));
        }
        config.set(LEGACY_NETHER_LAVA_LEVEL, null);
    }
}
