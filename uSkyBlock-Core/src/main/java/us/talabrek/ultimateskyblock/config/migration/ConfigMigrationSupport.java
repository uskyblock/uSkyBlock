package us.talabrek.ultimateskyblock.config.migration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.config.ConfigDuration;

import java.util.List;
import java.util.Locale;
import java.util.function.LongFunction;

final class ConfigMigrationSupport {
    private ConfigMigrationSupport() {
    }

    static void migrateSecondsToDuration(@NotNull YamlConfiguration config, @NotNull String path) {
        migrateToDuration(config, path, ConfigDuration::seconds);
    }

    static void migrateMinutesToDuration(@NotNull YamlConfiguration config, @NotNull String path) {
        migrateToDuration(config, path, ConfigDuration::minutes);
    }

    static void migrateMillisToDuration(@NotNull YamlConfiguration config, @NotNull String path) {
        migrateToDuration(config, path, ConfigDuration::millis);
    }

    static void migrateInviteTimeoutToDuration(@NotNull YamlConfiguration config, @NotNull String path) {
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

    static void setComment(@NotNull YamlConfiguration config, @NotNull String path, @NotNull String comment) {
        config.setComments(path, List.of(comment));
        config.setInlineComments(path, List.of());
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
