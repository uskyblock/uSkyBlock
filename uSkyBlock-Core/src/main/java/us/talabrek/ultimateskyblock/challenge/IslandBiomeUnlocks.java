package us.talabrek.ultimateskyblock.challenge;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeId;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRewards.BiomeReward;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRewards.RewardAction;
import us.talabrek.ultimateskyblock.challenge.catalog.RewardBundle;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.island.IslandInfo;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Island-scoped biome unlocks, derived from the island's completed challenges' biome rewards plus
 * the configured default set. Nothing is materialized: the unlock IS the challenge state, so
 * island create/restart resets unlocks with the progress, and editing a challenge's rewards
 * applies retroactively.
 *
 * <p>The personal {@code usb.biome.<key>} permission gate stays as an OR-fallback for donor ranks
 * and ops.</p>
 */
@Singleton
public class IslandBiomeUnlocks {
    private final ChallengeLogic challengeLogic;
    private final RuntimeConfigs runtimeConfigs;

    @Inject
    public IslandBiomeUnlocks(@NotNull ChallengeLogic challengeLogic, @NotNull RuntimeConfigs runtimeConfigs) {
        this.challengeLogic = Objects.requireNonNull(challengeLogic, "challengeLogic");
        this.runtimeConfigs = Objects.requireNonNull(runtimeConfigs, "runtimeConfigs");
    }

    public @NotNull Set<String> unlockedBiomes(@Nullable IslandInfo island) {
        Set<String> unlocked = new LinkedHashSet<>(runtimeConfigs.current().biomes().defaultUnlocked());
        if (island == null) {
            return unlocked;
        }
        Map<ChallengeId, ChallengeCompletion> progress = challengeLogic.completionLogic.getIslandChallenges(island.getName());
        for (Map.Entry<ChallengeId, ChallengeCompletion> entry : progress.entrySet()) {
            if (entry.getValue().getTimesCompleted() > 0) {
                challengeLogic.getDefinitionById(entry.getKey()).ifPresent(challenge -> {
                    collectBiomes(challenge.firstCompletionReward(), unlocked);
                    collectBiomes(challenge.repeatReward(), unlocked);
                });
            }
        }
        return unlocked;
    }

    public boolean isUnlocked(@Nullable IslandInfo island, @NotNull String biomeKey) {
        return unlockedBiomes(island).contains(biomeKey.toLowerCase(Locale.ROOT));
    }

    /**
     * The biome gate: a personal permission node or an island-earned unlock.
     */
    public boolean canUseBiome(@NotNull Player player, @Nullable IslandInfo island, @NotNull String biomeKey) {
        String normalized = biomeKey.toLowerCase(Locale.ROOT);
        return player.hasPermission("usb.biome." + normalized) || isUnlocked(island, normalized);
    }

    private static void collectBiomes(@NotNull RewardBundle bundle, @NotNull Set<String> unlocked) {
        for (RewardAction action : bundle.actions()) {
            if (action instanceof BiomeReward biomeReward) {
                unlocked.addAll(biomeReward.biomes());
            }
        }
    }
}
