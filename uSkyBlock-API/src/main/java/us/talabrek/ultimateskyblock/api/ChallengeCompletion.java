package us.talabrek.ultimateskyblock.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;

/**
 * Represents a challenge-completion.
 *
 * @since 2.7.0
 */
public interface ChallengeCompletion {
    /**
     * The name of the challenge.
     *
     * @return name of the challenge.
     * @since 2.7.0
     */
    String getName();

    /**
     * The timestamp at which the cooldown runs out.
     *
     * @return The timestamp at which the cooldown runs out.
     * @since 2.7.0
     * @deprecated Use {@link #cooldownUntil()} instead.
     */
    @Deprecated(since = "3.2.0")
    default long getCooldownUntil() {
        Instant cooldownUntil = cooldownUntil();
        return cooldownUntil == null ? 0 : cooldownUntil.toEpochMilli();
    }

    /**
     * The timestamp at which the cooldown runs out.
     *
     * @return The timestamp at which the cooldown runs out, or null if there is no cooldown.
     * @since 3.2.0
     */
    @Nullable Instant cooldownUntil();

    /**
     * Whether or not the challenge is currently on cooldown.
     *
     * @return Whether or not the challenge is currently on cooldown.
     * @since 2.7.0
     */
    boolean isOnCooldown();

    /**
     * How many milliseconds of the cooldown is left
     *
     * @return How many milliseconds of the cooldown is left
     * @since 2.7.0
     * @deprecated Use {@link #getCooldown()} instead.
     */
    @Deprecated(since = "3.2.0")
    default long getCooldownInMillis() {
        return getCooldown().toMillis();
    }

    /**
     * How much of the cooldown is left
     *
     * @return The duration of the cooldown that is left
     * @since 3.2.0
     */
    @NotNull Duration getCooldown();

    /**
     * Total number of times the challenge has been completed
     *
     * @return Total number of times the challenge has been completed
     * @since 2.7.0
     */
    int getTimesCompleted();

    /**
     * Number of times the challenge has been completed within this cooldown.
     *
     * @return Number of times the challenge has been completed within this cooldown.
     * @since 2.7.0
     */
    int getTimesCompletedInCooldown();
}
