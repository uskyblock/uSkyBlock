package us.talabrek.ultimateskyblock.challenge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeId;
import us.talabrek.ultimateskyblock.island.IslandKey;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SqliteChallengeProgressRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    public void storesAndLoadsChallengeProgress() {
        SqliteChallengeProgressRepository repository = new SqliteChallengeProgressRepository(
            tempDir.resolve("data").resolve("challenge-progress.db"),
            Logger.getAnonymousLogger()
        );
        IslandKey islandKey = IslandKey.fromIslandName("0,0");
        ChallengeId challengeKey = ChallengeId.of("cobblestonegenerator");

        Map<ChallengeId, ChallengeCompletion> progress = new HashMap<>();
        progress.put(challengeKey, new ChallengeCompletion(challengeKey, Instant.ofEpochMilli(1234L), 2, 1));

        repository.replace(islandKey, progress);

        Map<ChallengeId, ChallengeCompletion> loaded = repository.load(islandKey);
        assertTrue(repository.hasProgress(islandKey));
        assertEquals(2, loaded.get(challengeKey).getTimesCompleted());
        assertEquals(1, loaded.get(challengeKey).getTimesCompletedInCooldown());
        assertEquals(Instant.ofEpochMilli(1234L), loaded.get(challengeKey).cooldownUntil());
    }

    @Test
    public void storesAndLoadsProgressWithoutCooldown() {
        SqliteChallengeProgressRepository repository = new SqliteChallengeProgressRepository(
            tempDir.resolve("data").resolve("challenge-progress.db"),
            Logger.getAnonymousLogger()
        );
        IslandKey islandKey = IslandKey.fromIslandName("64,-32");
        ChallengeId challengeKey = ChallengeId.of("oncechallenge");

        Map<ChallengeId, ChallengeCompletion> progress = new HashMap<>();
        progress.put(challengeKey, new ChallengeCompletion(challengeKey, null, 4, 2));

        repository.replace(islandKey, progress);

        Map<ChallengeId, ChallengeCompletion> loaded = repository.load(islandKey);
        assertTrue(repository.hasProgress(islandKey));
        assertNull(loaded.get(challengeKey).cooldownUntil());
        assertEquals(4, loaded.get(challengeKey).getTimesCompleted());
        // Without an active cooldown the window counter collapses to 1 (completed) by design.
        assertEquals(1, loaded.get(challengeKey).getTimesCompletedInCooldown());
    }

    @Test
    public void omitsDefaultProgressRows() {
        SqliteChallengeProgressRepository repository = new SqliteChallengeProgressRepository(
            tempDir.resolve("data").resolve("challenge-progress.db"),
            Logger.getAnonymousLogger()
        );
        IslandKey islandKey = IslandKey.fromIslandName("128,256");
        ChallengeId challengeKey = ChallengeId.of("applecollector");

        Map<ChallengeId, ChallengeCompletion> progress = new HashMap<>();
        progress.put(challengeKey, new ChallengeCompletion(challengeKey, null, 0, 0));

        repository.replace(islandKey, progress);

        assertFalse(repository.hasProgress(islandKey));
        assertTrue(repository.load(islandKey).isEmpty());
    }

    @Test
    public void storesMetadata() {
        SqliteChallengeProgressRepository repository = new SqliteChallengeProgressRepository(
            tempDir.resolve("data").resolve("challenge-progress.db"),
            Logger.getAnonymousLogger()
        );

        repository.putMetadata("legacy_yaml_import_completed", "true");

        assertEquals("1", repository.getMetadata("schema_version").orElseThrow());
        assertEquals("true", repository.getMetadata("legacy_yaml_import_completed").orElseThrow());
    }
}
