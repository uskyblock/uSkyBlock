package us.talabrek.ultimateskyblock.challenge;

import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.island.IslandKey;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoadedChallengeProgressTest {
    private static final IslandKey ISLAND = IslandKey.fromIslandName("2,3");
    private static final ChallengeKey CHALLENGE_ID = ChallengeKey.of("testchallenge");

    @Test
    public void snapshotIsDefensiveCopy() {
        Map<ChallengeKey, ChallengeCompletion> initial = new HashMap<>();
        initial.put(CHALLENGE_ID, new ChallengeCompletion(CHALLENGE_ID, null, 1, 1));
        LoadedChallengeProgress loaded = new LoadedChallengeProgress(ISLAND, initial);

        Map<ChallengeKey, ChallengeCompletion> snapshot = loaded.snapshot();
        snapshot.get(CHALLENGE_ID).addTimesCompleted();
        snapshot.put(ChallengeKey.of("other"), new ChallengeCompletion(ChallengeKey.of("other"), null, 5, 0));

        Map<ChallengeKey, ChallengeCompletion> fresh = loaded.snapshot();
        assertEquals(1, fresh.get(CHALLENGE_ID).getTimesCompleted());
        assertFalse(fresh.containsKey(ChallengeKey.of("other")));
    }

    @Test
    public void onlyOneCompletionCanBeInFlight() {
        LoadedChallengeProgress loaded = new LoadedChallengeProgress(ISLAND, new HashMap<>());

        assertTrue(loaded.tryBeginCompletion());
        assertFalse(loaded.tryBeginCompletion());
        loaded.finishCompletion();
        assertTrue(loaded.tryBeginCompletion());
    }

    @Test
    public void writeLockBlocksCompletionsAndClearsInFlight() {
        LoadedChallengeProgress loaded = new LoadedChallengeProgress(ISLAND, new HashMap<>());
        assertTrue(loaded.tryBeginCompletion());

        loaded.lockWrites();

        assertFalse(loaded.isCompletionInFlight());
        assertTrue(loaded.isWriteLocked());
        assertFalse(loaded.tryBeginCompletion());
        loaded.unlockWrites();
        assertTrue(loaded.tryBeginCompletion());
    }

    @Test
    public void inFlightAndLockedEntriesAreNeverEvicted() {
        LoadedChallengeProgress loaded = new LoadedChallengeProgress(ISLAND, new HashMap<>());
        // Zero TTL: anything idle is immediately evictable.
        assertTrue(loaded.shouldEvict(Duration.ofMillis(-1)));

        assertTrue(loaded.tryBeginCompletion());
        assertFalse(loaded.shouldEvict(Duration.ofMillis(-1)));

        loaded.lockWrites();
        assertFalse(loaded.shouldEvict(Duration.ofMillis(-1)));
    }
}
