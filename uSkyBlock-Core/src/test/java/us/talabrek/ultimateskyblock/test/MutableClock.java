package us.talabrek.ultimateskyblock.test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

public class MutableClock extends Clock {
    private Instant instant;

    public MutableClock(Instant initialInstant) {
        this.instant = initialInstant;
    }

    @Override
    public ZoneId getZone() {
        return ZoneId.of("UTC");
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return instant;
    }

    public void advance(Duration duration) {
        this.instant = this.instant.plus(duration);
    }
}
