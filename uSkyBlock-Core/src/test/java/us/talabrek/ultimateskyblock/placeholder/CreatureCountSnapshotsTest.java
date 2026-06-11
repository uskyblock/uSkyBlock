package us.talabrek.ultimateskyblock.placeholder;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.api.IslandInfo;
import us.talabrek.ultimateskyblock.island.LimitLogic;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreatureCountSnapshotsTest {

    private Server server;
    private LimitLogic limitLogic;
    private Scheduler scheduler;
    private MutableClock clock;
    private CreatureCountSnapshots snapshots;
    private IslandInfo islandInfo;

    @BeforeEach
    public void setUp() throws Exception {
        server = BukkitServerMock.setupServerMock();
        limitLogic = mock(LimitLogic.class);
        scheduler = mock(Scheduler.class);
        clock = new MutableClock(Instant.parse("2026-06-11T12:00:00Z"));
        snapshots = new CreatureCountSnapshots(limitLogic, scheduler, clock);
        islandInfo = mock(IslandInfo.class);
        when(islandInfo.getName()).thenReturn("islandA");
        when(limitLogic.getCreatureCount(islandInfo))
            .thenReturn(Map.of(LimitLogic.CreatureType.GOLEM, 3));
    }

    @Test
    public void refreshesSynchronouslyOnMainThread() {
        when(server.isPrimaryThread()).thenReturn(true);

        Map<LimitLogic.CreatureType, Integer> counts = snapshots.counts(islandInfo);

        assertThat(counts.get(LimitLogic.CreatureType.GOLEM), is(3));
        verify(limitLogic, times(1)).getCreatureCount(islandInfo);
    }

    @Test
    public void servesFreshSnapshotWithoutRescan() {
        when(server.isPrimaryThread()).thenReturn(true);
        snapshots.counts(islandInfo);

        clock.advance(Duration.ofSeconds(5));
        snapshots.counts(islandInfo);

        verify(limitLogic, times(1)).getCreatureCount(islandInfo);
    }

    @Test
    public void rescansAfterMaxAgeOnMainThread() {
        when(server.isPrimaryThread()).thenReturn(true);
        snapshots.counts(islandInfo);

        clock.advance(Duration.ofSeconds(11));
        snapshots.counts(islandInfo);

        verify(limitLogic, times(2)).getCreatureCount(islandInfo);
    }

    @Test
    public void coldAsyncCallSchedulesRefreshAndReturnsNull() {
        when(server.isPrimaryThread()).thenReturn(false);

        assertNull(snapshots.counts(islandInfo));

        verify(limitLogic, never()).getCreatureCount(any());
        verify(scheduler, times(1)).sync(any(Runnable.class));
    }

    @Test
    public void staleAsyncCallReturnsLastSnapshotAndSchedulesOneRefresh() {
        when(server.isPrimaryThread()).thenReturn(true);
        snapshots.counts(islandInfo);
        clock.advance(Duration.ofSeconds(30));
        when(server.isPrimaryThread()).thenReturn(false);

        Map<LimitLogic.CreatureType, Integer> first = snapshots.counts(islandInfo);
        Map<LimitLogic.CreatureType, Integer> second = snapshots.counts(islandInfo);

        assertThat(first.get(LimitLogic.CreatureType.GOLEM), is(3));
        assertThat(second.get(LimitLogic.CreatureType.GOLEM), is(3));
        verify(limitLogic, times(1)).getCreatureCount(islandInfo); // only the initial main-thread scan
        verify(scheduler, times(1)).sync(any(Runnable.class));     // refresh deduplicated
    }

    @Test
    public void scheduledRefreshRunsScanAndClearsPendingFlag() {
        when(server.isPrimaryThread()).thenReturn(false);
        doAnswer(invocation -> {
            when(server.isPrimaryThread()).thenReturn(true);
            invocation.getArgument(0, Runnable.class).run();
            when(server.isPrimaryThread()).thenReturn(false);
            return null;
        }).when(scheduler).sync(any(Runnable.class));

        assertNull(snapshots.counts(islandInfo));            // triggers scheduled refresh (runs inline above)
        Map<LimitLogic.CreatureType, Integer> counts = snapshots.counts(islandInfo);

        assertThat(counts.get(LimitLogic.CreatureType.GOLEM), is(3));
        verify(limitLogic, times(1)).getCreatureCount(islandInfo);

        // The pending flag must be cleared: a later stale call schedules a SECOND refresh.
        clock.advance(Duration.ofSeconds(11));
        snapshots.counts(islandInfo);
        verify(scheduler, times(2)).sync(any(Runnable.class));
    }

    /** Minimal mutable Clock for TTL tests. */
    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }
}
