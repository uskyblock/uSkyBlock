package us.talabrek.ultimateskyblock.ittest;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeadlineTest {
    @Test
    void expiresAtBoundary() {
        MutableClock clock = new MutableClock();
        Deadline deadline = new Deadline(clock, Duration.ofSeconds(5));
        assertFalse(deadline.expired());
        clock.now = clock.now.plusSeconds(5);
        assertTrue(deadline.expired());
    }

    @Test
    void rejectsNonPositiveTimeout() {
        assertThrows(IllegalArgumentException.class, () -> new Deadline(Clock.systemUTC(), Duration.ZERO));
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.EPOCH;
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
