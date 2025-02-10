package us.talabrek.ultimateskyblock.challenge;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;

public class ChallengeCompletion implements us.talabrek.ultimateskyblock.api.ChallengeCompletion {
    private String name;
    private Instant cooldownUntil;
    private int timesCompleted;
    private int timesCompletedInCooldown;

    public ChallengeCompletion(final String name, @Nullable Instant cooldownUntil, final int timesCompleted, final int timesCompletedInCooldown) {
        super();
        this.name = name;
        this.cooldownUntil = cooldownUntil;
        this.timesCompleted = timesCompleted;
        this.timesCompletedInCooldown = timesCompletedInCooldown;
    }

    @Override
    public String getName() {
        return this.name;
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

    public void setName(final String name) {
        this.name = name;
    }
}
