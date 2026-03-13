package us.talabrek.ultimateskyblock.config.migration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class ConfigMigrationV117 implements ConfigMigration {
    private static final String LEGACY_MAX_SPAM = "general.maxSpam";
    private static final String CANONICAL_MAX_SPAM = "options.general.maxSpam";
    private static final String LEGACY_VISITOR_VEHICLE_DAMAGE = "options.protection.visitors.vehicle-break";
    private static final String CANONICAL_VISITOR_VEHICLE_DAMAGE = "options.protection.visitors.vehicle-damage";
    private static final String UNUSED_NETHER_ROOF = "options.protection.nether-roof";
    private static final String UNUSED_AWE_PROGRESS_MS = "asyncworldedit.progressEveryMs";
    private static final String UNUSED_AWE_PROGRESS_PCT = "asyncworldedit.progressEveryPct";
    private static final String UNUSED_NETHER_ACTIVATE_AT = "nether.activate-at";

    @Override
    public int fromVersion() {
        return 116;
    }

    @Override
    public int toVersion() {
        return 117;
    }

    @Override
    public void apply(@NotNull YamlConfiguration config, @NotNull Path pluginDataDir) {
        moveIntIfPresent(config, LEGACY_MAX_SPAM, CANONICAL_MAX_SPAM);
        moveBooleanIfPresent(config, LEGACY_VISITOR_VEHICLE_DAMAGE, CANONICAL_VISITOR_VEHICLE_DAMAGE);
        config.set(UNUSED_NETHER_ROOF, null);
        config.set(UNUSED_AWE_PROGRESS_MS, null);
        config.set(UNUSED_AWE_PROGRESS_PCT, null);
        config.set(UNUSED_NETHER_ACTIVATE_AT, null);
    }

    private static void moveIntIfPresent(@NotNull YamlConfiguration config, @NotNull String legacyPath, @NotNull String canonicalPath) {
        if (!config.isSet(legacyPath)) {
            return;
        }
        if (!config.isSet(canonicalPath)) {
            config.set(canonicalPath, config.getInt(legacyPath));
        }
        config.set(legacyPath, null);
    }

    private static void moveBooleanIfPresent(@NotNull YamlConfiguration config, @NotNull String legacyPath, @NotNull String canonicalPath) {
        if (!config.isSet(legacyPath)) {
            return;
        }
        if (!config.isSet(canonicalPath)) {
            config.set(canonicalPath, config.getBoolean(legacyPath));
        }
        config.set(legacyPath, null);
    }
}
