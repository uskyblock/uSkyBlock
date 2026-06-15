package us.talabrek.ultimateskyblock.challenge;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeId;

public class ChallengeCompletion implements us.talabrek.ultimateskyblock.api.ChallengeCompletion {
    private final ChallengeId id;
    private Instant cooldownUntil;
    private int timesCompleted;
    private int timesCompletedInCooldown;

    public ChallengeCompletion(@NotNull ChallengeId id, @Nullable Instant cooldownUntil, final int timesCompleted, final int timesCompletedInCooldown) {
        this.id = id;
        this.cooldownUntil = cooldownUntil;
        this.timesCompleted = timesCompleted;
        this.timesCompletedInCooldown = timesCompletedInCooldown;
    }

    public ChallengeId getId() {
        return id;
    }

    /**
     * @deprecated Use {@link #getId()} instead.
     */
    @Deprecated
    @Override
    public String getName() {
        return this.getId().value();
    }

    @Override
    public @Nullable Instant cooldownUntil() {
        return this.cooldownUntil;
    }

    @Override
    public boolean isOnCooldown() {
        return getCooldown().isPositive();
    }

    @Override
    public @NotNull Duration getCooldown() {
        if (cooldownUntil == null) {
            return Duration.ZERO;
        }
        Duration remainingCooldown = Duration.between(Instant.now(), cooldownUntil);
        return remainingCooldown.isNegative() ? Duration.ZERO : remainingCooldown;
    }

    @Override
    public int getTimesCompleted() {
        return this.timesCompleted;
    }

    public int getTimesCompletedInCooldown() {
        return isOnCooldown() ? this.timesCompletedInCooldown : timesCompleted > 0 ? 1 : 0;
    }

    public void setCooldownUntil(@Nullable Instant newCooldown) {
        this.cooldownUntil = newCooldown;
        this.timesCompletedInCooldown = 0;
    }

    public void setTimesCompleted(final int newCompleted) {
        this.timesCompleted = newCompleted;
        this.timesCompletedInCooldown = newCompleted;
    }

    public void addTimesCompleted() {
        ++this.timesCompleted;
        ++this.timesCompletedInCooldown;
    }
}
