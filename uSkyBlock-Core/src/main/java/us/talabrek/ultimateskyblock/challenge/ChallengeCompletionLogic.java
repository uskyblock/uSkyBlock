package us.talabrek.ultimateskyblock.challenge;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.island.IslandKey;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * Responsible for handling ChallengeCompletions
 */
public class ChallengeCompletionLogic {
    private final uSkyBlock plugin;
    private final ChallengeProgressRepository repository;
    private final Path legacyStorageDir;
    private final boolean legacyPlayerSharingConfigured;
    private final LoadingCache<IslandKey, Map<ChallengeKey, ChallengeCompletion>> completionCache;

    public ChallengeCompletionLogic(
        uSkyBlock plugin,
        RuntimeConfigs runtimeConfigs,
        FileConfiguration config,
        ChallengeProgressRepository repository
    ) {
        this.plugin = plugin;
        this.repository = repository;
        this.legacyStorageDir = plugin.getDataFolder().toPath().resolve("completion");
        legacyPlayerSharingConfigured = config.getString("challengeSharing", "island").equalsIgnoreCase("player");
        if (legacyPlayerSharingConfigured) {
            plugin.getLogger().warning("Legacy challengeSharing=player is deprecated. Challenge progress will be migrated to island-owned database records.");
        }
        completionCache = CacheBuilder
            .from(runtimeConfigs.current().advanced().completionCacheSpec())
            .removalListener((RemovalListener<IslandKey, Map<ChallengeKey, ChallengeCompletion>>) removal -> {
                if (removal.getKey() != null && removal.getValue() != null) {
                    repository.replace(removal.getKey(), removal.getValue());
                }
            })
            .build(new CacheLoader<>() {
                       @Override
                       public @NotNull Map<ChallengeKey, ChallengeCompletion> load(@NotNull IslandKey id) {
                           return loadOrPopulateProgress(id);
                       }
                   }
            );
        if (!Files.exists(legacyStorageDir)) {
            legacyStorageDir.toFile().mkdirs();
        }
        Map<IslandKey, Map<ChallengeKey, ChallengeCompletion>> migratedProgress =
            new ChallengeProgressMigration(plugin, repository).migrateLegacyDataEagerly();
        for (Map.Entry<IslandKey, Map<ChallengeKey, ChallengeCompletion>> entry : migratedProgress.entrySet()) {
            completionCache.put(entry.getKey(), entry.getValue());
        }
    }

    private Map<ChallengeKey, ChallengeCompletion> loadOrPopulateProgress(IslandKey islandKey) {
        Map<ChallengeKey, ChallengeCompletion> challengeMap = new HashMap<>();
        plugin.getChallengeLogic().populateChallenges(challengeMap);
        challengeMap.putAll(repository.load(islandKey));
        return challengeMap;
    }

    public Map<ChallengeKey, ChallengeCompletion> getIslandChallenges(String islandName) {
        if (islandName == null) {
            return new HashMap<>();
        }
        return getCachedChallenges(IslandKey.fromIslandName(islandName));
    }

    public Map<ChallengeKey, ChallengeCompletion> getChallenges(PlayerInfo playerInfo) {
        if (playerInfo == null || !playerInfo.getHasIsland() || playerInfo.locationForParty() == null) {
            return new HashMap<>();
        }
        return getCachedChallenges(getIslandKey(playerInfo));
    }

    public void completeChallenge(PlayerInfo playerInfo, ChallengeKey id) {
        Map<ChallengeKey, ChallengeCompletion> challenges = getChallenges(playerInfo);
        if (challenges.containsKey(id)) {
            ChallengeCompletion completion = challenges.get(id);
            if (!completion.isOnCooldown()) {
                ChallengeLogic challengeLogic = plugin.getChallengeLogic();
                Duration resetDuration = challengeLogic.getChallengeById(id).orElseThrow().getResetDuration();
                if (resetDuration.isPositive()) {
                    Instant now = Instant.now();
                    completion.setCooldownUntil(now.plus(resetDuration));
                } else {
                    completion.setCooldownUntil(null);
                }
            }
            completion.addTimesCompleted();
        }
    }

    public void resetChallenge(PlayerInfo playerInfo, ChallengeKey id) {
        Map<ChallengeKey, ChallengeCompletion> challenges = getChallenges(playerInfo);
        if (challenges.containsKey(id)) {
            challenges.get(id).setTimesCompleted(0);
            challenges.get(id).setCooldownUntil(null);
        }
    }

    public int checkChallenge(PlayerInfo playerInfo, ChallengeKey id) {
        Map<ChallengeKey, ChallengeCompletion> challenges = getChallenges(playerInfo);
        if (challenges.containsKey(id)) {
            return challenges.get(id).getTimesCompleted();
        }
        return 0;
    }

    public ChallengeCompletion getChallenge(PlayerInfo playerInfo, ChallengeKey id) {
        Map<ChallengeKey, ChallengeCompletion> challenges = getChallenges(playerInfo);
        return challenges.get(id);
    }

    public void resetAllChallenges(PlayerInfo playerInfo) {
        Map<ChallengeKey, ChallengeCompletion> challengeMap = new HashMap<>();
        plugin.getChallengeLogic().populateChallenges(challengeMap);
        IslandKey islandKey = getIslandKey(playerInfo);
        completionCache.put(islandKey, challengeMap);
    }

    public void shutdown() {
        flushCache();
        repository.shutdown();
    }

    public long flushCache() {
        long size = completionCache.size();
        completionCache.invalidateAll();
        return size;
    }

    public boolean isIslandSharing() {
        return true;
    }

    private @NotNull IslandKey getIslandKey(@NotNull PlayerInfo playerInfo) {
        return IslandKey.fromIslandName(playerInfo.locationForParty());
    }

    private @NotNull Map<ChallengeKey, ChallengeCompletion> getCachedChallenges(@NotNull IslandKey islandKey) {
        try {
            return completionCache.get(islandKey);
        } catch (ExecutionException e) {
            plugin.getLogger().log(Level.WARNING, "Error fetching challenge-completion for id " + islandKey.value(), e);
            return new HashMap<>();
        }
    }
}
