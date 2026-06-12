package us.talabrek.ultimateskyblock.challenge;

import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeId;
import us.talabrek.ultimateskyblock.island.IslandKey;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class LoadedChallengeProgress {
    private final IslandKey islandKey;
    private Map<ChallengeId, ChallengeCompletion> progress;
    private Instant lastAccessAt;
    private boolean completionInFlight;
    private boolean writeLocked;

    public LoadedChallengeProgress(@NotNull IslandKey islandKey, @NotNull Map<ChallengeId, ChallengeCompletion> progress) {
        this.islandKey = Objects.requireNonNull(islandKey, "islandKey");
        this.progress = copyProgress(progress);
        this.lastAccessAt = Instant.now();
    }

    public @NotNull IslandKey islandKey() {
        return islandKey;
    }

    public synchronized @NotNull Map<ChallengeId, ChallengeCompletion> snapshot() {
        touch();
        return copyProgress(progress);
    }

    public synchronized void replace(@NotNull Map<ChallengeId, ChallengeCompletion> newProgress) {
        this.progress = copyProgress(newProgress);
        touch();
    }

    public synchronized boolean tryBeginCompletion() {
        touch();
        if (completionInFlight || writeLocked) {
            return false;
        }
        completionInFlight = true;
        return true;
    }

    public synchronized void finishCompletion() {
        completionInFlight = false;
        touch();
    }

    public synchronized void lockWrites() {
        writeLocked = true;
        completionInFlight = false;
        touch();
    }

    public synchronized void unlockWrites() {
        writeLocked = false;
        touch();
    }

    public synchronized boolean isCompletionInFlight() {
        return completionInFlight;
    }

    public synchronized boolean isWriteLocked() {
        return writeLocked;
    }

    public synchronized boolean shouldEvict(@NotNull Duration idleTtl) {
        return !completionInFlight && !writeLocked && lastAccessAt.plus(idleTtl).isBefore(Instant.now());
    }

    public synchronized void touch() {
        lastAccessAt = Instant.now();
    }

    public static @NotNull Map<ChallengeId, ChallengeCompletion> copyProgress(@NotNull Map<ChallengeId, ChallengeCompletion> source) {
        Map<ChallengeId, ChallengeCompletion> copy = new HashMap<>();
        for (Map.Entry<ChallengeId, ChallengeCompletion> entry : source.entrySet()) {
            ChallengeCompletion completion = entry.getValue();
            copy.put(entry.getKey(), new ChallengeCompletion(
                completion.getId(),
                completion.cooldownUntil(),
                completion.getTimesCompleted(),
                completion.getTimesCompletedInCooldown()
            ));
        }
        return copy;
    }
}
