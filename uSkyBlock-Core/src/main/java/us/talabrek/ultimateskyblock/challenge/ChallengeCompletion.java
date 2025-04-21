package us.talabrek.ultimateskyblock.challenge;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public class ChallengeCompletion implements us.talabrek.ultimateskyblock.api.ChallengeCompletion {
    private final us.talabrek.ultimateskyblock.api.model.ChallengeCompletion completion;

    public ChallengeCompletion(@NotNull us.talabrek.ultimateskyblock.api.model.ChallengeCompletion completion) {
        Objects.requireNonNull(completion);
        this.completion = completion;
    }

    @Override
    public String getName() {
        return completion.getChallenge();
    }

    @Override
    public @Nullable Instant cooldownUntil() {
        return completion.getCooldownUntil();
    }

    @Override
    public boolean isOnCooldown() {
        return getCooldown().isPositive();
    }

    @Override
    public @NotNull Duration getCooldown() {
        if (completion.getCooldownUntil() == null) {
            return Duration.ZERO;
        }

        Duration remainingCooldown = Duration.between(Instant.now(), completion.getCooldownUntil());
        return remainingCooldown.isNegative() ? Duration.ZERO : remainingCooldown;
    }

    @Override
    public long getCooldownInMillis() {
        if (completion.getCooldownUntil().toEpochMilli() < 0) {
            return -1;
        }
        long now = System.currentTimeMillis();
        return completion.getCooldownUntil().toEpochMilli() > now ? completion.getCooldownUntil().toEpochMilli() - now : 0;
    }

    @Override
    public int getTimesCompleted() {
        return completion.getTimesCompleted();
    }

    public int getTimesCompletedInCooldown() {
        return isOnCooldown() ? completion.getTimesCompletedInCooldown() : completion.getTimesCompleted() > 0 ? 1 : 0;
    }

    public void setCooldownUntil(@Nullable Instant newCooldown) {
        completion.setCooldownUntil(newCooldown);
        completion.setTimesCompletedInCooldown(0);
    }

    public void setTimesCompleted(final int newCompleted) {
        completion.setTimesCompleted(newCompleted);
        completion.setTimesCompletedInCooldown(newCompleted);
    }

    public void addTimesCompleted() {
        completion.addTimesCompleted();
        completion.addTimesCompletedInCooldown();
    }
}
