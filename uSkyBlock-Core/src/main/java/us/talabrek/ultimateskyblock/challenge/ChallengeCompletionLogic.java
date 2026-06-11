package us.talabrek.ultimateskyblock.challenge;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.island.IslandKey;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Handles challenge progress access and migration. Hot completion flow is handled by {@link ChallengeExecutor}.
 */
public class ChallengeCompletionLogic {
    /**
     * Config flag written by the legacy challenges.yml importer when the server previously ran
     * challengeSharing=player; consumed (and cleared) by the one-shot SQLite progress migration.
     */
    public static final String LEGACY_PLAYER_SHARING_CONFIG_KEY = "options.challenges.legacy-player-sharing";

    private final uSkyBlock plugin;
    private final ChallengeLogic challengeLogic;
    private final Scheduler scheduler;
    private final ChallengeProgressRepository repository;
    private final ChallengeProgressCache progressCache;

    public ChallengeCompletionLogic(
        @NotNull ChallengeLogic challengeLogic,
        @NotNull uSkyBlock plugin,
        @NotNull Scheduler scheduler,
        @NotNull RuntimeConfigs runtimeConfigs,
        @NotNull ChallengeProgressRepository repository
    ) {
        this.challengeLogic = Objects.requireNonNull(challengeLogic, "challengeLogic");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.progressCache = new ChallengeProgressCache(plugin.getLogger(), scheduler, repository, challengeLogic);
        boolean legacyPlayerSharing = plugin.getConfig().getBoolean(LEGACY_PLAYER_SHARING_CONFIG_KEY, false);
        new ChallengeProgressMigration(plugin, challengeLogic, repository, legacyPlayerSharing).migrateLegacyDataEagerly();
        clearLegacyPlayerSharingFlag();
    }

    private void clearLegacyPlayerSharingFlag() {
        if (plugin.getConfig().contains(LEGACY_PLAYER_SHARING_CONFIG_KEY)) {
            plugin.getConfig().set(LEGACY_PLAYER_SHARING_CONFIG_KEY, null);
            plugin.saveConfig();
        }
    }

    public @NotNull ChallengeProgressCache progressCache() {
        return progressCache;
    }

    public void whenChallengesLoaded(@Nullable PlayerInfo playerInfo, @NotNull Runnable onLoaded, @NotNull Consumer<Throwable> onError) {
        if (playerInfo == null || !playerInfo.getHasIsland() || playerInfo.locationForParty() == null) {
            onError.accept(new IllegalStateException("Player has no island"));
            return;
        }
        IslandKey islandKey = getIslandKey(playerInfo);
        progressCache.loadAsync(islandKey).whenComplete((ignored, error) -> scheduler.sync(() -> {
            if (error != null) {
                onError.accept(error);
            } else {
                onLoaded.run();
            }
        }));
    }

    public Map<ChallengeKey, ChallengeCompletion> getIslandChallenges(@Nullable String islandName) {
        if (islandName == null) {
            return new HashMap<>();
        }
        return getLoadedOrLoad(IslandKey.fromIslandName(islandName));
    }

    public Map<ChallengeKey, ChallengeCompletion> getChallenges(@Nullable PlayerInfo playerInfo) {
        if (playerInfo == null || !playerInfo.getHasIsland() || playerInfo.locationForParty() == null) {
            return new HashMap<>();
        }
        return getLoadedOrLoad(getIslandKey(playerInfo));
    }

    private @NotNull Map<ChallengeKey, ChallengeCompletion> getLoadedOrLoad(@NotNull IslandKey islandKey) {
        try {
            return loadForWrite(islandKey);
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.WARNING, "Error fetching challenge-completion for id " + islandKey.value(), e);
            return new HashMap<>();
        }
    }

    /**
     * Loads without degradation: mutation paths must fail loudly on a repository error rather
     * than write a near-default map over the island's stored progress.
     */
    private @NotNull Map<ChallengeKey, ChallengeCompletion> loadForWrite(@NotNull IslandKey islandKey) {
        return progressCache.getIfLoaded(islandKey)
            .map(LoadedChallengeProgress::snapshot)
            .orElseGet(() -> progressCache.loadSynchronously(islandKey));
    }

    public void completeChallenge(@NotNull PlayerInfo playerInfo, @NotNull ChallengeKey id) {
        IslandKey islandKey = getIslandKey(playerInfo);
        Map<ChallengeKey, ChallengeCompletion> challenges = loadForWrite(islandKey);
        ChallengeCompletion completion = challenges.computeIfAbsent(id, key -> new ChallengeCompletion(key, null, 0, 0));
        if (!completion.isOnCooldown()) {
            Duration resetDuration = challengeLogic.getChallengeById(id).orElseThrow().getResetDuration();
            if (resetDuration.isPositive()) {
                completion.setCooldownUntil(Instant.now().plus(resetDuration));
            } else {
                completion.setCooldownUntil(null);
            }
        }
        completion.addTimesCompleted();
        storeSynchronously(islandKey, challenges);
    }

    public void resetChallenge(@NotNull PlayerInfo playerInfo, @NotNull ChallengeKey id) {
        IslandKey islandKey = getIslandKey(playerInfo);
        Map<ChallengeKey, ChallengeCompletion> challenges = loadForWrite(islandKey);
        ChallengeCompletion completion = challenges.computeIfAbsent(id, key -> new ChallengeCompletion(key, null, 0, 0));
        completion.setTimesCompleted(0);
        completion.setCooldownUntil(null);
        storeSynchronously(islandKey, challenges);
    }

    public int checkChallenge(@NotNull PlayerInfo playerInfo, @NotNull ChallengeKey id) {
        return getChallenges(playerInfo).getOrDefault(id, new ChallengeCompletion(id, null, 0, 0)).getTimesCompleted();
    }

    public @Nullable ChallengeCompletion getChallenge(@NotNull PlayerInfo playerInfo, @NotNull ChallengeKey id) {
        return getChallenges(playerInfo).get(id);
    }

    public void resetAllChallenges(@NotNull PlayerInfo playerInfo) {
        Map<ChallengeKey, ChallengeCompletion> challengeMap = new HashMap<>();
        challengeLogic.populateChallenges(challengeMap);
        storeSynchronously(getIslandKey(playerInfo), challengeMap);
    }

    public void shutdown() {
        progressCache.shutdown();
        repository.shutdown();
    }

    public long flushCache() {
        return progressCache.clearLoaded();
    }

    private void storeSynchronously(@NotNull IslandKey islandKey, @NotNull Map<ChallengeKey, ChallengeCompletion> progress) {
        repository.replace(islandKey, progress);
        progressCache.replaceLoaded(islandKey, progress);
    }

    private @NotNull IslandKey getIslandKey(@NotNull PlayerInfo playerInfo) {
        return IslandKey.fromIslandName(playerInfo.locationForParty());
    }
}
