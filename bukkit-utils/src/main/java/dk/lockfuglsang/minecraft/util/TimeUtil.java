package dk.lockfuglsang.minecraft.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

public enum TimeUtil {
    ;
    private static final Pattern TIME_PATTERN = Pattern.compile("((?<d>[0-9]+)d)?\\s*((?<h>[0-9]+)h)?\\s*((?<m>[0-9]+)m)?\\s*((?<s>[0-9]+)s)?\\s*((?<ms>[0-9]+)ms)?");

    public static @Nullable Duration stringAsDuration(@NotNull String specification) {
        Matcher matcher = TIME_PATTERN.matcher(specification);
        if (matcher.matches()) {
            Duration result = Duration.ZERO;
            if (matcher.group("d") != null) {
                result = result.plusDays(Long.parseLong(matcher.group("d")));
            }
            if (matcher.group("h") != null) {
                result = result.plusHours(Long.parseLong(matcher.group("h")));
            }
            if (matcher.group("m") != null) {
                result = result.plusMinutes(Long.parseLong(matcher.group("m")));
            }
            if (matcher.group("s") != null) {
                result = result.plusSeconds(Long.parseLong(matcher.group("s")));
            }
            if (matcher.group("ms") != null) {
                result = result.plusMillis(Long.parseLong(matcher.group("ms")));
            }
            return result;
        }
        return null;
    }

    public static @NotNull String durationAsString(@NotNull Duration duration) {
        String result = "";
        if (duration.toDaysPart() > 0) {
            result += " " + duration.toDaysPart() + tr("d");
        }
        if (duration.toHoursPart() > 0) {
            result += " " + duration.toHoursPart() + tr("h");
        }
        if (duration.toMinutesPart() > 0) {
            result += " " + duration.toMinutesPart() + tr("m");
        }
        if (duration.toSecondsPart() > 0 || result.isEmpty()) {
            result += " " + duration.toSecondsPart() + tr("s");
        }
        return result.trim();
    }

    public static @NotNull String durationAsShort(@NotNull Duration duration) {
        long m = duration.toMinutes();
        long s = duration.toSecondsPart();
        long ms = duration.toMillisPart();
        return String.format(tr("{0,number,0}:{1,number,00}.{2,number,000}", m, s, ms));
    }

    public static long durationAsTicks(@NotNull Duration duration) {
        return duration.toMillis() / 50;
    }

    public static @NotNull Duration ticksAsDuration(long ticks) {
        return Duration.ofMillis(ticks * 50);
    }
}
