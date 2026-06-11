package us.talabrek.ultimateskyblock.placeholder;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.api.IslandInfo;
import us.talabrek.ultimateskyblock.island.LimitLogic;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main-thread-confined snapshots of {@link LimitLogic#getCreatureCount(IslandInfo)}.
 * The underlying entity scan is expensive and only legal on the main thread, while
 * placeholder lookups arrive from async scoreboard threads. Snapshots are refreshed
 * on the main thread at most every {@link #MAX_AGE}; async callers get the last
 * snapshot, or null while a scheduled refresh is pending.
 */
@Singleton
public class CreatureCountSnapshots {

    private static final Duration MAX_AGE = Duration.ofSeconds(10);

    private record Snapshot(@NotNull Map<LimitLogic.CreatureType, Integer> counts, @NotNull Instant takenAt) {
    }

    private final Cache<String, Snapshot> snapshots = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterAccess(Duration.ofMinutes(5))
        .build();
    private final Set<String> pendingRefresh = ConcurrentHashMap.newKeySet();
    private final LimitLogic limitLogic;
    private final Scheduler scheduler;
    private final Clock clock;

    @Inject
    public CreatureCountSnapshots(@NotNull LimitLogic limitLogic, @NotNull Scheduler scheduler) {
        this(limitLogic, scheduler, Clock.systemUTC());
    }

    CreatureCountSnapshots(@NotNull LimitLogic limitLogic, @NotNull Scheduler scheduler, @NotNull Clock clock) {
        this.limitLogic = limitLogic;
        this.scheduler = scheduler;
        this.clock = clock;
    }

    /**
     * Returns the creature counts for the given island, or null if no snapshot is
     * available yet and the caller is off the main thread (a refresh is scheduled).
     */
    public @Nullable Map<LimitLogic.CreatureType, Integer> counts(@NotNull IslandInfo islandInfo) {
        String island = islandInfo.getName();
        Snapshot snapshot = snapshots.getIfPresent(island);
        if (snapshot != null && Duration.between(snapshot.takenAt(), clock.instant()).compareTo(MAX_AGE) < 0) {
            return snapshot.counts();
        }
        if (Bukkit.isPrimaryThread()) {
            return refresh(islandInfo).counts();
        }
        if (pendingRefresh.add(island)) {
            try {
                scheduler.sync(() -> {
                    try {
                        refresh(islandInfo);
                    } finally {
                        pendingRefresh.remove(island);
                    }
                });
            } catch (RuntimeException e) {
                // Scheduler rejects tasks while the plugin is disabling; stale-or-null is fine then.
                pendingRefresh.remove(island);
            }
        }
        return snapshot != null ? snapshot.counts() : null;
    }

    private @NotNull Snapshot refresh(@NotNull IslandInfo islandInfo) {
        Snapshot snapshot = new Snapshot(Map.copyOf(limitLogic.getCreatureCount(islandInfo)), clock.instant());
        snapshots.put(islandInfo.getName(), snapshot);
        return snapshot;
    }
}
