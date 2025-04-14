package us.talabrek.ultimateskyblock.api.model;

import java.time.Instant;
import java.util.UUID;

public class ChallengeCompletion extends Model {
    protected final UUID uuid;
    protected final String challenge;
    protected Instant cooldownUntil = Instant.EPOCH;
    protected int timesCompleted = 0;
    protected int timesCompletedInCooldown = 0;

    public ChallengeCompletion(UUID uuid, String challenge) {
        this.uuid = uuid;
        this.challenge = challenge;
    }

    public ChallengeCompletion(UUID uuid,  String challenge, Instant firstCompleted, int timesCompleted, int timesCompletedSinceTimer) {
        this.uuid = uuid;
        this.challenge = challenge;
        this.cooldownUntil = firstCompleted;
        this.timesCompleted = timesCompleted;
        this.timesCompletedInCooldown = timesCompletedSinceTimer;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getChallenge() {
        return challenge;
    }

    public Instant getCooldownUntil() {
        return cooldownUntil;
    }

    public void setCooldownUntil(Instant cooldownUntil) {
        this.cooldownUntil = cooldownUntil;
        setDirty(true);
    }

    public boolean isOnCooldown() {
        Instant now = Instant.now();
        return this.cooldownUntil.toEpochMilli() < 0 || this.cooldownUntil.isAfter(now);
    }

    public int getTimesCompleted() {
        return timesCompleted;
    }

    public void addTimesCompleted() {
        this.timesCompleted++;
        setDirty(true);
    }

    public void setTimesCompleted(int timesCompleted) {
        this.timesCompleted = timesCompleted;
        setDirty(true);
    }

    public int getTimesCompletedInCooldown() {
        return timesCompletedInCooldown;
    }

    public void addTimesCompletedInCooldown() {
        this.timesCompletedInCooldown++;
        setDirty(true);
    }

    public void setTimesCompletedInCooldown(int timesCompletedInCooldown) {
        this.timesCompletedInCooldown = timesCompletedInCooldown;
        setDirty(true);
    }
}
