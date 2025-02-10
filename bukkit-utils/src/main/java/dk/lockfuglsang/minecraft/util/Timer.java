package dk.lockfuglsang.minecraft.util;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;

public class Timer {

    private final Instant start;

    public Timer(@NotNull Instant start) {
        this.start = start;
    }

    public Instant getStart() {
        return start;
    }

    public Duration elapsed() {
        return Duration.between(start, Instant.now());
    }

    public String elapsedAsString() {
        return TimeUtil.durationAsString(elapsed());
    }

    public static Timer start() {
        return new Timer(Instant.now());
    }
}
