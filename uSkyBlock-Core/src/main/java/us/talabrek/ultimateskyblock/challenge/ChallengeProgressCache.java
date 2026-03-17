package us.talabrek.ultimateskyblock.challenge;

import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.island.IslandKey;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

public final class ChallengeProgressCache {
    private static final Duration IDLE_TTL = Duration.ofMinutes(10);
    private static final Duration EVICTION_INTERVAL = Duration.ofMinutes(2);

    private final Logger logger;
    private final Scheduler scheduler;
    private final ChallengeProgressRepository repository;
    private final ChallengeLogic challengeLogic;
    private final ConcurrentMap<IslandKey, LoadedChallengeProgress> loaded = new ConcurrentHashMap<>();
    private final ConcurrentMap<IslandKey, CompletableFuture<LoadedChallengeProgress>> loading = new ConcurrentHashMap<>();
    private final @Nullable BukkitTask evictionTask;

    public ChallengeProgressCache(
        @NotNull Logger logger,
        @NotNull Scheduler scheduler,
        @NotNull ChallengeProgressRepository repository,
        @NotNull ChallengeLogic challengeLogic
    ) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.challengeLogic = Objects.requireNonNull(challengeLogic, "challengeLogic");
        this.evictionTask = scheduler.async(this::evictIdleEntries, EVICTION_INTERVAL, EVICTION_INTERVAL);
    }

    public @NotNull Optional<LoadedChallengeProgress> getIfLoaded(@NotNull IslandKey islandKey) {
        LoadedChallengeProgress progress = loaded.get(Objects.requireNonNull(islandKey, "islandKey"));
        if (progress != null) {
            progress.touch();
        }
        return Optional.ofNullable(progress);
    }

    public @NotNull CompletableFuture<LoadedChallengeProgress> loadAsync(@NotNull IslandKey islandKey) {
        Objects.requireNonNull(islandKey, "islandKey");
        LoadedChallengeProgress existing = loaded.get(islandKey);
        if (existing != null) {
            existing.touch();
            return CompletableFuture.completedFuture(existing);
        }
        return loading.computeIfAbsent(islandKey, this::startLoad);
    }

    public void replaceLoaded(@NotNull IslandKey islandKey, @NotNull Map<ChallengeKey, ChallengeCompletion> progress) {
        loaded.compute(islandKey, (key, existing) -> {
            if (existing == null) {
                return new LoadedChallengeProgress(key, progress);
            }
            existing.replace(progress);
            return existing;
        });
    }

    public @NotNull Map<ChallengeKey, ChallengeCompletion> loadSynchronously(@NotNull IslandKey islandKey) {
        Map<ChallengeKey, ChallengeCompletion> progress = new HashMap<>();
        challengeLogic.populateChallenges(progress);
        progress.putAll(repository.load(islandKey));
        return progress;
    }

    public int clearLoaded() {
        int size = loaded.size();
        loaded.clear();
        return size;
    }

    public void shutdown() {
        if (evictionTask != null) {
            evictionTask.cancel();
        }
        loaded.clear();
        loading.clear();
    }

    private @NotNull CompletableFuture<LoadedChallengeProgress> startLoad(@NotNull IslandKey islandKey) {
        CompletableFuture<LoadedChallengeProgress> future = new CompletableFuture<>();
        scheduler.async(() -> {
            try {
                LoadedChallengeProgress progress = new LoadedChallengeProgress(islandKey, loadSynchronously(islandKey));
                loaded.put(islandKey, progress);
                future.complete(progress);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            } finally {
                loading.remove(islandKey, future);
            }
        });
        return future;
    }

    private void evictIdleEntries() {
        loaded.entrySet().removeIf(entry -> entry.getValue().shouldEvict(IDLE_TTL));
    }
}
