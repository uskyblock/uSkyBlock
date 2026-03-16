package us.talabrek.ultimateskyblock.challenge;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.island.IslandKey;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
    private final Set<IslandKey> initializedOwners = ConcurrentHashMap.newKeySet();
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
    }

    private Map<ChallengeKey, ChallengeCompletion> loadOrPopulateProgress(IslandKey islandKey) {
        Map<ChallengeKey, ChallengeCompletion> challengeMap = new HashMap<>();
        plugin.getChallengeLogic().populateChallenges(challengeMap);
        challengeMap.putAll(repository.load(islandKey));
        return challengeMap;
    }

    private Map<ChallengeKey, ChallengeCompletion> loadFromConfiguration(ConfigurationSection root) {
        Map<ChallengeKey, ChallengeCompletion> challengeMap = new HashMap<>();
        if (root != null) {
            for (String challengeName : root.getKeys(false)) {
                long firstCompleted = root.getLong(challengeName + ".firstCompleted", 0);
                Instant firstCompletedDuration = firstCompleted > 0 ? Instant.ofEpochMilli(firstCompleted) : null;
                ChallengeKey challengeId = ChallengeKey.of(challengeName);
                challengeMap.put(challengeId, new ChallengeCompletion(
                    challengeId,
                    firstCompletedDuration,
                    root.getInt(challengeName + ".timesCompleted", 0),
                    root.getInt(challengeName + ".timesCompletedSinceTimer", 0)
                ));
            }
        }
        return challengeMap;
    }

    public Map<ChallengeKey, ChallengeCompletion> getIslandChallenges(String islandName) {
        if (islandName == null) {
            return new HashMap<>();
        }
        IslandKey islandKey = IslandKey.fromIslandName(islandName);
        ensureInitialized(islandKey, null);
        return getCachedChallenges(islandKey);
    }

    public Map<ChallengeKey, ChallengeCompletion> getChallenges(PlayerInfo playerInfo) {
        if (playerInfo == null || !playerInfo.getHasIsland() || playerInfo.locationForParty() == null) {
            return new HashMap<>();
        }
        IslandKey islandKey = getIslandKey(playerInfo);
        ensureInitialized(islandKey, playerInfo);
        return getCachedChallenges(islandKey);
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
        initializedOwners.add(islandKey);
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

    private void ensureInitialized(@NotNull IslandKey islandKey, PlayerInfo playerInfo) {
        if (initializedOwners.contains(islandKey)) {
            return;
        }
        if (repository.hasProgress(islandKey)) {
            initializedOwners.add(islandKey);
            return;
        }

        Map<ChallengeKey, ChallengeCompletion> imported = loadOrPopulateProgress(islandKey);
        boolean migrated = importLegacyFileProgress(islandKey, imported);
        if (!migrated && playerInfo != null) {
            migrated = importLegacyPlayerProgress(playerInfo, imported);
        }
        if (migrated) {
            repository.replace(islandKey, imported);
            completionCache.put(islandKey, imported);
        }
        initializedOwners.add(islandKey);
    }

    private boolean importLegacyPlayerProgress(@NotNull PlayerInfo playerInfo, @NotNull Map<ChallengeKey, ChallengeCompletion> current) {
        Map<ChallengeKey, ChallengeCompletion> imported = loadFromConfiguration(playerInfo.getConfig().getConfigurationSection("player.challenges"));
        if (imported.isEmpty()) {
            return false;
        }
        current.putAll(imported);
        playerInfo.getConfig().set("player.challenges", null);
        playerInfo.save();
        plugin.getLogger().info("Migrated legacy player challenge progress for " + playerInfo.getPlayerName() + ".");
        return true;
    }

    private boolean importLegacyFileProgress(@NotNull IslandKey islandKey, @NotNull Map<ChallengeKey, ChallengeCompletion> current) {
        Path islandFile = legacyStorageDir.resolve(islandKey.value() + ".yml");
        if (Files.exists(islandFile)) {
            Map<ChallengeKey, ChallengeCompletion> imported = loadLegacyFile(islandFile.toFile());
            current.putAll(imported);
            backupLegacyFile(islandFile);
            plugin.getLogger().info("Migrated legacy island challenge progress for " + islandKey.value() + ".");
            return true;
        }

        IslandInfo islandInfo = plugin.getIslandInfo(islandKey.value());
        if (islandInfo != null && islandInfo.getLeaderUniqueId() != null) {
            Path leaderFile = legacyStorageDir.resolve(islandInfo.getLeaderUniqueId() + ".yml");
            if (Files.exists(leaderFile)) {
                Map<ChallengeKey, ChallengeCompletion> imported = loadLegacyFile(leaderFile.toFile());
                current.putAll(imported);
                backupLegacyFile(leaderFile);
                plugin.getLogger().info("Migrated legacy leader challenge progress for " + islandKey.value() + ".");
                return true;
            }
        }
        return false;
    }

    private @NotNull Map<ChallengeKey, ChallengeCompletion> loadLegacyFile(@NotNull File configFile) {
        FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(configFile);
        if (fileConfiguration.getRoot() == null) {
            return Collections.emptyMap();
        }
        return loadFromConfiguration(fileConfiguration.getRoot());
    }

    private void backupLegacyFile(@NotNull Path source) {
        try {
            Path backupDir = legacyStorageDir.resolve("legacy-backup");
            Files.createDirectories(backupDir);
            Path target = backupDir.resolve(source.getFileName());
            int suffix = 1;
            while (Files.exists(target)) {
                target = backupDir.resolve(source.getFileName().toString().replace(".yml", "-" + suffix + ".yml"));
                suffix++;
            }
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Unable to back up legacy challenge progress file " + source, e);
        }
    }
}
