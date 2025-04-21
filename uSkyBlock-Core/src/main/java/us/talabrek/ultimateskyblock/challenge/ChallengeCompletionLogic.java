package us.talabrek.ultimateskyblock.challenge;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.time.Duration;
import java.time.Instant;

import us.talabrek.ultimateskyblock.api.model.ChallengeCompletionSet;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Responsible for handling ChallengeCompletions
 */
public class ChallengeCompletionLogic {
    private final uSkyBlock plugin;
    private final boolean storeOnIsland;
    private final LoadingCache<UUID, ChallengeCompletionSet> completionSetCache;

    public ChallengeCompletionLogic(uSkyBlock plugin, FileConfiguration config) {
        this.plugin = plugin;
        storeOnIsland = config.getString("challengeSharing", "island").equalsIgnoreCase("island");
        completionSetCache = CacheBuilder
            .from(plugin.getConfig().getString("options.advanced.completionCache", "maximumSize=200,expireAfterWrite=15m,expireAfterAccess=10m"))
            .removalListener((RemovalListener<UUID, ChallengeCompletionSet>) removal ->
                plugin.getStorage().saveChallengeCompletion(removal.getValue()))
            .build(
                new CacheLoader<>() {
                    @Override
                    public @NotNull ChallengeCompletionSet load(@NotNull UUID uuid) {
                        ChallengeCompletionSet set = plugin.getStorage().getChallengeCompletion(uuid).join();
                        return Objects.requireNonNullElseGet(set, () -> new ChallengeCompletionSet(uuid, plugin.getChallengeLogic().getSharingType()));
                    }
                }
            );
    }

    public UUID getSharingUuid(PlayerInfo playerInfo) {
        if (plugin.getChallengeLogic().isIslandSharing()) {
            return plugin.getStorage().getPlayerIsland(playerInfo.getUniqueId()).join();
        }

        return playerInfo.getUniqueId();
    }

    public ChallengeCompletionSet getIslandChallenges(String islandName) {
        UUID islandUuid = plugin.getStorage().getIslandByName(islandName).join();
        if (storeOnIsland && islandUuid != null) {
            try {
                return completionSetCache.get(islandUuid);
            } catch (ExecutionException ex) {
                plugin.getLog4JLogger().warn("Error fetching challenge-completion for id {}", islandName, ex);
            }
        }

        return null;
    }

    public ChallengeCompletionSet getChallenges(PlayerInfo playerInfo) {
        if (playerInfo == null) {
            return null;
        }

        try {
            return completionSetCache.get(getSharingUuid(playerInfo));
        } catch (ExecutionException ex) {
            plugin.getLog4JLogger().warn("Error fetching challenge-completion for id {}", playerInfo.getUniqueId(), ex);
        }
        return new ChallengeCompletionSet(playerInfo.getUniqueId(), plugin.getChallengeLogic().getSharingType());
    }

    public void completeChallenge(PlayerInfo playerInfo, String challengeName) {
        try {
            ChallengeCompletionSet completionSet = completionSetCache.get(getSharingUuid(playerInfo));
            completionSet.getCompletionMap().values().forEach(completion -> {
                if (!completion.isOnCooldown()) {
                    Duration resetDuration = plugin.getChallengeLogic().getResetDuration(challengeName);
                    if (resetDuration.isPositive()) {
                        Instant now = Instant.now();
                        completion.setCooldownUntil(now.plus(resetDuration));
                    } else {
                        completion.setCooldownUntil(null);
                    }
                }
                completion.addTimesCompleted();
                completion.addTimesCompletedInCooldown();
            });

        } catch (ExecutionException ex) {
            plugin.getLog4JLogger().warn("Error completing challenge-completion {} for player {}", challengeName, playerInfo.getUniqueId(), ex);
        }
    }

    public void resetChallenge(PlayerInfo playerInfo, String challengeName) {
        try {
            ChallengeCompletionSet set = completionSetCache.get(getSharingUuid(playerInfo));
            if (set.getCompletion(challengeName) != null) {
                set.getCompletion(challengeName).setTimesCompleted(0);
                set.getCompletion(challengeName).setCooldownUntil(null);
            }
        } catch (ExecutionException ex) {
            plugin.getLog4JLogger().warn("Error resetting challenge-completion for id {}", challengeName, ex);
        }
    }

    public int checkChallenge(PlayerInfo playerInfo, String challengeName) {
        try {
            ChallengeCompletionSet set = completionSetCache.get(getSharingUuid(playerInfo));
            if (set.getCompletion(challengeName) != null) {
                return set.getCompletion(challengeName).getTimesCompleted();
            }
        } catch (ExecutionException ex) {
            plugin.getLog4JLogger().warn("Error checking challenge-completion for id {}", challengeName, ex);
        }

        return 0;
    }

    public ChallengeCompletion getChallenge(PlayerInfo playerInfo, String challenge) {
        try {
            return new ChallengeCompletion(completionSetCache.get(getSharingUuid(playerInfo)).getCompletion(challenge));
        } catch (ExecutionException ex) {
            plugin.getLog4JLogger().warn("Error fetching challenge-completion for id {}", challenge, ex);
        }

        return null;
    }

    private ChallengeCompletionSet populateChallenges(ChallengeCompletionSet set) {
        plugin.getChallengeLogic().getRanks().forEach(rank -> rank.getChallenges().forEach(challenge ->
            set.setCompletion(
                challenge.getName().toLowerCase(),
                new us.talabrek.ultimateskyblock.api.model.ChallengeCompletion(set.getUuid(), challenge.getName().toLowerCase()))));
        return set;
    }

    public void resetAllChallenges(PlayerInfo playerInfo) {
        try {
            ChallengeCompletionSet set = completionSetCache.get(getSharingUuid(playerInfo));
            set.reset();
            completionSetCache.put(playerInfo.getPlayerId(), populateChallenges(set));
        } catch (ExecutionException ex) {
            plugin.getLog4JLogger().warn("Error resetting challenge-completion for UUID {}", playerInfo.getPlayerId(), ex);
        }
    }

    public void shutdown() {
        flushCache();
    }

    public long flushCache() {
        long size = completionSetCache.size();
        completionSetCache.invalidateAll();
        return size;
    }

    public boolean isIslandSharing() {
        return storeOnIsland;
    }
}
