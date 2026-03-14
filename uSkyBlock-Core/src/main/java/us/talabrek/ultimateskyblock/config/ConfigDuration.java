package us.talabrek.ultimateskyblock.config;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum ConfigDuration {
    ;

    private static final Pattern DURATION_PATTERN = Pattern.compile("^(?<value>-?[0-9]+)(?<unit>ms|s|m|h|d)$");

    @NotNull
    public static Duration parse(@NotNull String value) {
        Matcher matcher = DURATION_PATTERN.matcher(value.trim().toLowerCase(Locale.ROOT));
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid duration value: " + value);
        }

        long amount = Long.parseLong(matcher.group("value"));
        return switch (matcher.group("unit")) {
            case "ms" -> Duration.ofMillis(amount);
            case "s" -> Duration.ofSeconds(amount);
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            case "d" -> Duration.ofDays(amount);
            default -> throw new IllegalArgumentException("Unsupported duration unit: " + matcher.group("unit"));
        };
    }

    @NotNull
    public static String seconds(long seconds) {
        return seconds + "s";
    }

    @NotNull
    public static String minutes(long minutes) {
        return minutes + "m";
    }

    @NotNull
    public static String millis(long millis) {
        return millis + "ms";
    }
}
