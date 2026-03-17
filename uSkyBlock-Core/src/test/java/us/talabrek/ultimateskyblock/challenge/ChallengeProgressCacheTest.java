package us.talabrek.ultimateskyblock.challenge;

import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.island.IslandKey;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChallengeProgressCacheTest {
    @Test
    void loadAsyncDeduplicatesAndCachesLoadedProgress() {
        ChallengeKey challengeKey = ChallengeKey.of("cobblestonegenerator");
        AtomicInteger loadCalls = new AtomicInteger();

        ChallengeLogic challengeLogic = mock(ChallengeLogic.class);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Map<ChallengeKey, ChallengeCompletion> progress = invocation.getArgument(0);
            progress.put(challengeKey, new ChallengeCompletion(challengeKey, null, 0, 0));
            return null;
        }).when(challengeLogic).populateChallenges(any());

        ChallengeProgressRepository repository = mock(ChallengeProgressRepository.class);
        when(repository.load(IslandKey.fromIslandName("0,0"))).thenAnswer(invocation -> {
            loadCalls.incrementAndGet();
            return Map.of(challengeKey, new ChallengeCompletion(challengeKey, null, 2, 1));
        });

        ChallengeProgressCache cache = new ChallengeProgressCache(
            Logger.getAnonymousLogger(),
            inlineScheduler(),
            repository,
            challengeLogic
        );

        IslandKey islandKey = IslandKey.fromIslandName("0,0");
        LoadedChallengeProgress first = cache.loadAsync(islandKey).join();
        LoadedChallengeProgress second = cache.loadAsync(islandKey).join();

        assertEquals(1, loadCalls.get());
        assertEquals(first, second);
        assertTrue(cache.getIfLoaded(islandKey).isPresent());
        assertEquals(2, first.snapshot().get(challengeKey).getTimesCompleted());
    }

    @Test
    void replaceLoadedUpdatesWarmSnapshot() {
        ChallengeKey challengeKey = ChallengeKey.of("cobblestonegenerator");
        ChallengeLogic challengeLogic = mock(ChallengeLogic.class);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Map<ChallengeKey, ChallengeCompletion> progress = invocation.getArgument(0);
            progress.put(challengeKey, new ChallengeCompletion(challengeKey, null, 0, 0));
            return null;
        }).when(challengeLogic).populateChallenges(any());

        ChallengeProgressCache cache = new ChallengeProgressCache(
            Logger.getAnonymousLogger(),
            inlineScheduler(),
            mock(ChallengeProgressRepository.class),
            challengeLogic
        );

        IslandKey islandKey = IslandKey.fromIslandName("0,0");
        Map<ChallengeKey, ChallengeCompletion> progress = new HashMap<>();
        progress.put(challengeKey, new ChallengeCompletion(challengeKey, null, 3, 1));

        cache.replaceLoaded(islandKey, progress);

        assertEquals(3, cache.getIfLoaded(islandKey).orElseThrow().snapshot().get(challengeKey).getTimesCompleted());
    }

    private Scheduler inlineScheduler() {
        Scheduler scheduler = mock(Scheduler.class);
        BukkitTask task = mock(BukkitTask.class);
        when(scheduler.async(any(Runnable.class))).thenAnswer(invocation -> {
            CompletableFuture.runAsync(invocation.getArgument(0, Runnable.class));
            return task;
        });
        when(scheduler.async(any(Runnable.class), any(java.time.Duration.class), any(java.time.Duration.class)))
            .thenReturn(task);
        return scheduler;
    }
}
