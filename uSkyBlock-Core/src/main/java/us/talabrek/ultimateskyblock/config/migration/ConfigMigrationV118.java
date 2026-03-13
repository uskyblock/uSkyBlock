package us.talabrek.ultimateskyblock.config.migration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class ConfigMigrationV118 implements ConfigMigration {
    private static final String COOLDOWN_INFO = "options.general.cooldownInfo";
    private static final String MAX_SPAM = "options.general.maxSpam";
    private static final String AUTO_REFRESH_SCORE = "options.island.autoRefreshScore";
    private static final String INIT_DELAY = "init.initDelay";
    private static final String ISLAND_SAVE_EVERY = "options.advanced.island.saveEvery";
    private static final String PLAYER_SAVE_EVERY = "options.advanced.player.saveEvery";
    private static final String FEEDBACK_EVERY = "async.long.feedbackEvery";
    private static final String PURGE_TIMEOUT = "options.advanced.purgeTimeout";
    private static final String PLAYERDB_SAVE_DELAY = "playerdb.saveDelay";
    private static final String ASYNC_MAX_MS = "async.maxMs";
    private static final String ASYNC_YIELD_DELAY = "async.yieldDelay";
    private static final String IMPORTER_PROGRESS_EVERY_MS = "importer.progressEveryMs";
    private static final String LEGACY_AWE_HEART_BEAT_MS = "asyncworldedit.watchDog.heartBeatMs";
    private static final String CANONICAL_AWE_HEART_BEAT = "asyncworldedit.watchDog.heartBeat";

    @Override
    public int fromVersion() {
        return 117;
    }

    @Override
    public int toVersion() {
        return 118;
    }

    @Override
    public void apply(@NotNull YamlConfiguration config, @NotNull Path pluginDataDir) {
        convertSecondsDuration(config, COOLDOWN_INFO);
        convertMillisDuration(config, MAX_SPAM);
        convertMinutesDuration(config, AUTO_REFRESH_SCORE);
        convertTicksDuration(config, INIT_DELAY);
        convertSecondsDuration(config, ISLAND_SAVE_EVERY);
        convertSecondsDuration(config, PLAYER_SAVE_EVERY);
        convertMillisDuration(config, FEEDBACK_EVERY);
        convertMillisDuration(config, PURGE_TIMEOUT);
        convertMillisDuration(config, PLAYERDB_SAVE_DELAY);
        convertMillisDuration(config, ASYNC_MAX_MS);
        convertTicksDuration(config, ASYNC_YIELD_DELAY);
        convertMillisDuration(config, IMPORTER_PROGRESS_EVERY_MS);
        moveMillisDuration(config, LEGACY_AWE_HEART_BEAT_MS, CANONICAL_AWE_HEART_BEAT);
    }

    private static void convertSecondsDuration(@NotNull YamlConfiguration config, @NotNull String path) {
        convertDuration(config, path, "s");
    }

    private static void convertMinutesDuration(@NotNull YamlConfiguration config, @NotNull String path) {
        convertDuration(config, path, "m");
    }

    private static void convertMillisDuration(@NotNull YamlConfiguration config, @NotNull String path) {
        convertDuration(config, path, "ms");
    }

    private static void convertTicksDuration(@NotNull YamlConfiguration config, @NotNull String path) {
        if (!config.isSet(path)) {
            return;
        }
        Object value = config.get(path);
        if (value instanceof Number number) {
            config.set(path, Math.max(0L, number.longValue()) * 50L + "ms");
        }
    }

    private static void moveMillisDuration(@NotNull YamlConfiguration config, @NotNull String legacyPath, @NotNull String canonicalPath) {
        if (!config.isSet(legacyPath)) {
            return;
        }
        if (!config.isSet(canonicalPath)) {
            Object value = config.get(legacyPath);
            if (value instanceof Number number) {
                config.set(canonicalPath, Math.max(0L, number.longValue()) + "ms");
            } else if (value instanceof String stringValue) {
                config.set(canonicalPath, stringValue);
            }
        }
        config.set(legacyPath, null);
    }

    private static void convertDuration(@NotNull YamlConfiguration config, @NotNull String path, @NotNull String unitSuffix) {
        if (!config.isSet(path)) {
            return;
        }
        Object value = config.get(path);
        if (value instanceof Number number) {
            config.set(path, Math.max(0L, number.longValue()) + unitSuffix);
        }
    }
}
