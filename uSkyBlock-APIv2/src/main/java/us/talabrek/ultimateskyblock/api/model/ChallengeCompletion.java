package us.talabrek.ultimateskyblock.api.model;

import java.time.Instant;
import java.util.UUID;

public class ChallengeCompletion extends Model {
    protected final UUID uuid;
    protected final CompletionSharing sharingType;
    protected final String challenge;
    protected Instant firstCompleted = Instant.MIN;
    protected int timesCompleted = 0;
    protected int timesCompletedSinceTimer = 0;

    public ChallengeCompletion(UUID uuid, CompletionSharing sharingType, String challenge) {
        this.uuid = uuid;
        this.sharingType = sharingType;
        this.challenge = challenge;
    }

    public ChallengeCompletion(UUID uuid, CompletionSharing sharingType, String challenge, Instant firstCompleted, int timesCompleted, int timesCompletedSinceTimer) {
        this.uuid = uuid;
        this.sharingType = sharingType;
        this.challenge = challenge;
        this.firstCompleted = firstCompleted;
        this.timesCompleted = timesCompleted;
        this.timesCompletedSinceTimer = timesCompletedSinceTimer;
    }

    public UUID getUuid() {
        return uuid;
    }

    public CompletionSharing getSharingType() {
        return sharingType;
    }

    public String getChallenge() {
        return challenge;
    }

    public Instant getFirstCompleted() {
        return firstCompleted;
    }

    public void setFirstCompleted(Instant firstCompleted) {
        this.firstCompleted = firstCompleted;
        setDirty(true);
    }

    public int getTimesCompleted() {
        return timesCompleted;
    }

    public void setTimesCompleted(int timesCompleted) {
        this.timesCompleted = timesCompleted;
        setDirty(true);
    }

    public int getTimesCompletedSinceTimer() {
        return timesCompletedSinceTimer;
    }

    public void setTimesCompletedSinceTimer(int timesCompletedSinceTimer) {
        this.timesCompletedSinceTimer = timesCompletedSinceTimer;
        setDirty(true);
    }

    public enum CompletionSharing {
        ISLAND,
        PLAYER
    }
}
