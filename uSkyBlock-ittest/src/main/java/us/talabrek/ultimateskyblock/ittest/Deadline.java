package us.talabrek.ultimateskyblock.ittest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public final class Deadline {
    private final Clock clock;
    private final Instant expiresAt;

    public Deadline(Clock clock, Duration timeout) {
        if (timeout.isNegative() || timeout.isZero()) throw new IllegalArgumentException("timeout must be positive");
        this.clock = clock;
        this.expiresAt = clock.instant().plus(timeout);
    }

    public boolean expired() {
        return !clock.instant().isBefore(expiresAt);
    }

    public Duration remaining() {
        Duration remaining = Duration.between(clock.instant(), expiresAt);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }
}
