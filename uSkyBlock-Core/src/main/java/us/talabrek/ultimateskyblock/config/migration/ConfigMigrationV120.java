package us.talabrek.ultimateskyblock.config.migration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class ConfigMigrationV120 implements ConfigMigration {
    private static final String LEGACY_PROGRESS_PCT = "importer.progressEveryPct";
    private static final String CANONICAL_PROGRESS_FRACTION = "importer.progressEveryFraction";

    @Override
    public int fromVersion() {
        return 119;
    }

    @Override
    public int toVersion() {
        return 120;
    }

    @Override
    public void apply(@NotNull YamlConfiguration config, @NotNull Path pluginDataDir) {
        if (!config.isSet(LEGACY_PROGRESS_PCT)) {
            return;
        }

        if (!config.isSet(CANONICAL_PROGRESS_FRACTION)) {
            Double legacyPct = readLegacyPercent(config.get(LEGACY_PROGRESS_PCT));
            if (legacyPct != null) {
                config.set(CANONICAL_PROGRESS_FRACTION, legacyPct / 100d);
            }
        }
        config.set(LEGACY_PROGRESS_PCT, null);
    }

    private static Double readLegacyPercent(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String string) {
            try {
                return Double.parseDouble(string.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
