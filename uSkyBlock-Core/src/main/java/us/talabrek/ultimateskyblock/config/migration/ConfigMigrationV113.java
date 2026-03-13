package us.talabrek.ultimateskyblock.config.migration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.config.ConfigDuration;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.function.LongFunction;

public final class ConfigMigrationV113 implements ConfigMigration {
    @Override
    public int fromVersion() {
        return 112;
    }

    @Override
    public int toVersion() {
        return 113;
    }

    @Override
    public void apply(@NotNull YamlConfiguration config, @NotNull Path pluginDataDir) {
        migrateSecondsToDuration(config, "options.general.cooldownRestart");
        migrateSecondsToDuration(config, "options.general.biomeChange");
        migrateSecondsToDuration(config, "options.island.islandTeleportDelay");
        migrateMinutesToDuration(config, "options.island.topTenTimeout");
        migrateInviteTimeoutToDuration(config, "options.party.invite-timeout");
        migrateSecondsToDuration(config, "options.advanced.confirmTimeout");
        migrateMillisToDuration(config, "options.restart.teleportDelay");

        setComment(config, "options.general.cooldownRestart",
            "# [duration] The time before a player can use the /island restart command again. Use ms, s, m, h, or d.");
        setComment(config, "options.general.biomeChange",
            "# [duration] The time before a player can use the /island biome command again. Use ms, s, m, h, or d.");
        setComment(config, "options.island.islandTeleportDelay",
            "# [duration] The delay before teleporting a player to their island. Use ms, s, m, h, or d.");
        setComment(config, "options.island.topTenTimeout",
            "# [duration] How long to cache top-ten data before recalculating it. Use ms, s, m, h, or d.");
        setComment(config, "options.party.invite-timeout",
            "# [duration] How long an island invite stays valid. Use ms, s, m, h, or d.");
        setComment(config, "options.advanced.confirmTimeout",
            "# [duration] The time to wait for repeating a risky command. Use ms, s, m, h, or d.");
        setComment(config, "options.restart.teleportDelay",
            "# [duration] The time to wait before porting the player back on /is restart or /is create. Use ms, s, m, h, or d.");
    }

    private static void migrateSecondsToDuration(@NotNull YamlConfiguration config, @NotNull String path) {
        migrateToDuration(config, path, ConfigDuration::seconds);
    }

    private static void migrateMinutesToDuration(@NotNull YamlConfiguration config, @NotNull String path) {
        migrateToDuration(config, path, ConfigDuration::minutes);
    }

    private static void migrateMillisToDuration(@NotNull YamlConfiguration config, @NotNull String path) {
        migrateToDuration(config, path, ConfigDuration::millis);
    }

    private static void migrateInviteTimeoutToDuration(@NotNull YamlConfiguration config, @NotNull String path) {
        Object value = config.get(path);
        if (value instanceof Number number) {
            config.set(path, formatInviteTimeout(number.longValue()));
            return;
        }
        if (value instanceof String text) {
            String trimmed = text.trim();
            if (trimmed.matches("[0-9]+")) {
                config.set(path, formatInviteTimeout(Long.parseLong(trimmed)));
                return;
            }
            throw new IllegalStateException("Cannot migrate invite-timeout at " + path + " from non-numeric value: " + value);
        }
        throw new IllegalStateException("Cannot migrate config duration at " + path + ": " + value);
    }

    private static void setComment(@NotNull YamlConfiguration config, @NotNull String path, @NotNull String comment) {
        config.setComments(path, List.of(comment));
    }

    @NotNull
    private static String formatInviteTimeout(long rawValue) {
        if (rawValue >= 1000) {
            if (rawValue % 1000 == 0) {
                return ConfigDuration.seconds(rawValue / 1000);
            }
            return ConfigDuration.millis(rawValue);
        }
        return ConfigDuration.seconds(rawValue);
    }

    private static void migrateToDuration(@NotNull YamlConfiguration config, @NotNull String path,
                                          @NotNull LongFunction<String> formatter) {
        Object value = config.get(path);
        if (value instanceof Number number) {
            config.set(path, formatter.apply(number.longValue()));
            return;
        }
        if (value instanceof String text) {
            String trimmed = text.trim();
            if (trimmed.matches("[0-9]+")) {
                config.set(path, formatter.apply(Long.parseLong(trimmed)));
                return;
            }
            ConfigDuration.parse(trimmed);
            config.set(path, trimmed.toLowerCase(Locale.ROOT));
            return;
        }
        throw new IllegalStateException("Cannot migrate config duration at " + path + ": " + value);
    }
}
