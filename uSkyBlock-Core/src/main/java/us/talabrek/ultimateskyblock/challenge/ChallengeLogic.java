package us.talabrek.ultimateskyblock.challenge;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.bootstrap.PluginLog;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.api.event.MemberJoinedEvent;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeCatalog;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeId;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRewards;
import us.talabrek.ultimateskyblock.challenge.catalog.RankDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.RankId;
import us.talabrek.ultimateskyblock.challenge.catalog.RewardBundle;
import us.talabrek.ultimateskyblock.challenge.catalog.bootstrap.ChallengeCatalogLoader;
import us.talabrek.ultimateskyblock.hook.HookManager;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.PerkLogic;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * The home of challenge business logic.
 */
@Singleton
public class ChallengeLogic implements Listener {
    private final Logger logger;
    private final uSkyBlock plugin;
    private final RuntimeConfigs runtimeConfigs;
    private final PerkLogic perkLogic;
    private final HookManager hookManager;

    private final ChallengeCatalog catalog;
    private final ChallengeUnlockEvaluator unlockEvaluator;

    public final ChallengeCompletionLogic completionLogic;
    private final ChallengeExecutor challengeExecutor;

    @Inject
    public ChallengeLogic(
        @NotNull @PluginLog Logger logger,
        @NotNull uSkyBlock plugin,
        @NotNull RuntimeConfigs runtimeConfigs,
        @NotNull PerkLogic perkLogic,
        @NotNull HookManager hookManager,
        @NotNull ChallengeCatalogLoader challengeCatalogLoader,
        @NotNull Scheduler scheduler,
        @NotNull ChallengeProgressRepository challengeProgressRepository
    ) {
        this.logger = logger;
        this.plugin = plugin;
        this.runtimeConfigs = runtimeConfigs;
        this.perkLogic = perkLogic;
        this.hookManager = hookManager;
        this.catalog = challengeCatalogLoader.load();
        this.unlockEvaluator = new ChallengeUnlockEvaluator(catalog);
        completionLogic = new ChallengeCompletionLogic(this, plugin, scheduler, runtimeConfigs, challengeProgressRepository);
        RewardApplier rewardApplier = new RewardApplier(plugin, runtimeConfigs, hookManager, perkLogic);
        challengeExecutor = new ChallengeExecutor(logger, plugin, scheduler, this, unlockEvaluator, rewardApplier,
            completionLogic.progressCache(), challengeProgressRepository);
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public @NotNull ChallengeCatalog getCatalog() {
        return catalog;
    }

    public @NotNull ChallengeUnlockEvaluator getUnlockEvaluator() {
        return unlockEvaluator;
    }

    /**
     * Catalog definition lookup by canonical id.
     */
    public Optional<ChallengeDefinition> getDefinitionById(@NotNull ChallengeKey key) {
        return catalog.challenge(ChallengeId.of(key.id()));
    }

    public boolean isEnabled() {
        return runtimeConfigs.current().challenges().enabled();
    }

    /**
     * Result object for challenge lookup that distinguishes between FOUND, NOT_FOUND, and AMBIGUOUS.
     */
    public static final class ChallengeLookupResult {
        public enum Status {FOUND, NOT_FOUND, AMBIGUOUS}

        private final Status status;
        private final @Nullable ChallengeDefinition challenge;
        private final @NotNull List<String> suggestions;
        private final @NotNull String normalizedInput;

        private ChallengeLookupResult(
            Status status,
            @Nullable ChallengeDefinition challenge,
            @NotNull List<String> suggestions,
            @NotNull String normalizedInput
        ) {
            this.status = status;
            this.challenge = challenge;
            this.suggestions = List.copyOf(suggestions);
            this.normalizedInput = normalizedInput;
        }

        public static ChallengeLookupResult found(@NotNull ChallengeDefinition c, @NotNull String normalizedInput) {
            return new ChallengeLookupResult(Status.FOUND, c, Collections.emptyList(), normalizedInput);
        }

        public static ChallengeLookupResult notFound(@NotNull String normalizedInput) {
            return new ChallengeLookupResult(Status.NOT_FOUND, null, Collections.emptyList(), normalizedInput);
        }

        public static ChallengeLookupResult ambiguous(@NotNull List<String> suggestions, @NotNull String normalizedInput) {
            return new ChallengeLookupResult(Status.AMBIGUOUS, null, suggestions, normalizedInput);
        }

        public Status getStatus() {
            return status;
        }

        public @Nullable ChallengeDefinition getChallenge() {
            return challenge;
        }

        public @NotNull ChallengeKey getChallengeKey() {
            Objects.requireNonNull(challenge, "challenge");
            return ChallengeKey.of(challenge.id().value());
        }

        public @NotNull List<String> getSuggestions() {
            return suggestions;
        }

        public @NotNull String getNormalizedInput() {
            return normalizedInput;
        }
    }

    // Fixed matching parameters (kept simple as per design decision)
    private static final int MIN_PREFIX_LENGTH = 6;
    private static final int MAX_SUGGESTIONS = 5;



    /**
     * Resolve a user-provided input into a challenge, returning detailed status.
     * Fuzzy find using the following matching rules, in that order:
     * 1) exact internal id (case-insensitive)
     * 2) exact display name (color-stripped, case-insensitive)
     * 3) exact slug (lowercase, no whitespace) against id and display
     * 4) prefix on slug (case-insensitive), requires unique match; otherwise AMBIGUOUS
     */
    public @NotNull ChallengeLookupResult resolveChallenge(@NotNull String userInput) {

        String inputLower = normalizeLower(userInput);
        String inputSlug = toSlug(userInput);
        if (inputLower.isEmpty()) {
            return ChallengeLookupResult.notFound(inputLower);
        }

        // 1) exact internal id
        Optional<ChallengeDefinition> exactId = catalog.challenge(ChallengeId.of(inputLower));
        if (exactId.isPresent()) {
            return ChallengeLookupResult.found(exactId.get(), inputLower);
        }

        Collection<ChallengeDefinition> all = catalog.index().challengesById().values();

        // 2) exact display name (formatting-stripped)
        for (ChallengeDefinition c : all) {
            if (normalizeLower(ChallengeText.plainName(c)).equals(inputLower)) {
                return ChallengeLookupResult.found(c, inputLower);
            }
        }

        // 3) exact slug match – collect candidates
        List<ChallengeDefinition> candidates = new ArrayList<>();
        for (ChallengeDefinition c : all) {
            if (toSlug(c.id().value()).equals(inputSlug) || toSlug(ChallengeText.plainName(c)).equals(inputSlug)) {
                candidates.add(c);
            }
        }
        if (candidates.size() == 1) {
            return ChallengeLookupResult.found(candidates.getFirst(), inputLower);
        } else if (candidates.size() > 1) {
            return ChallengeLookupResult.ambiguous(toSuggestionList(candidates, MAX_SUGGESTIONS), inputLower);
        }

        // 4) unique prefix on slug
        if (inputSlug.length() >= MIN_PREFIX_LENGTH) {
            for (ChallengeDefinition c : all) {
                String idSlug = toSlug(c.id().value());
                String dnSlug = toSlug(ChallengeText.plainName(c));
                if (idSlug.startsWith(inputSlug) || dnSlug.startsWith(inputSlug)) {
                    candidates.add(c);
                }
            }
            if (candidates.size() == 1) {
                return ChallengeLookupResult.found(candidates.getFirst(), inputLower);
            } else if (candidates.size() > 1) {
                return ChallengeLookupResult.ambiguous(toSuggestionList(candidates, MAX_SUGGESTIONS), inputLower);
            }
        }

        return ChallengeLookupResult.notFound(inputLower);
    }

    private static String normalizeLower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).trim();
    }

    private static String toSlug(String s) {
        if (s == null) return "";
        return normalizeLower(s).replaceAll("\\s+", "");
    }

    private static List<String> toSuggestionList(List<ChallengeDefinition> candidates, int max) {
        List<String> list = new ArrayList<>();
        for (ChallengeDefinition c : candidates) {
            list.add(ChallengeText.plainName(c));
            if (list.size() >= max) break;
        }
        return list;
    }

    public @NotNull List<ChallengeKey> getAvailableChallenges(PlayerInfo playerInfo) {
        List<ChallengeKey> list = new ArrayList<>();
        if (playerInfo == null || !playerInfo.getHasIsland()) {
            return list;
        }
        ChallengeUnlockEvaluator.UnlockContext context = unlockContextFor(playerInfo);
        for (ChallengeDefinition challenge : catalog.index().challengesById().values()) {
            if (unlockEvaluator.isChallengeUnlocked(challenge, context)) {
                list.add(ChallengeKey.of(challenge.id().value()));
            }
        }
        return list;
    }

    /**
     * Builds an unlock context from the player's island progress; permission checks degrade to
     * false when the player is offline.
     */
    public @NotNull ChallengeUnlockEvaluator.UnlockContext unlockContextFor(@NotNull PlayerInfo playerInfo) {
        Player player = playerInfo.getPlayer();
        IslandInfo islandInfo = playerInfo.getIslandInfo();
        return new ChallengeUnlockEvaluator.UnlockContext(
            completionLogic.getChallenges(playerInfo),
            player != null ? player::hasPermission : permission -> false,
            islandInfo != null ? islandInfo.getLevel() : 0d
        );
    }

    public @NotNull List<ChallengeKey> getAllChallengeIds() {
        return catalog.index().challengesById().keySet().stream()
            .map(id -> ChallengeKey.of(id.value()))
            .toList();
    }

    public @NotNull List<ChallengeDefinition> getChallengesForRank(String rank) {
        return catalog.rank(RankId.of(rank)).map(RankDefinition::challenges).orElse(List.of());
    }

    public void completeChallenge(@NotNull Player player, @NotNull ChallengeKey id) {
        challengeExecutor.attempt(player, id);
    }

    public void completeChallenge(@NotNull Player player, @NotNull ChallengeKey id, @NotNull List<Inventory> itemSources) {
        challengeExecutor.attempt(player, id, itemSources);
    }

    public void completeChallenge(@NotNull Player player, @NotNull ChallengeKey id, @NotNull List<Inventory> itemSources, @NotNull Runnable onSettled) {
        challengeExecutor.attempt(player, id, itemSources, onSettled);
    }

    public void whenChallengesLoaded(@Nullable PlayerInfo playerInfo, @NotNull Runnable onLoaded, @NotNull Consumer<Throwable> onError) {
        completionLogic.whenChallengesLoaded(playerInfo, onLoaded, onError);
    }

    public int getCountOf(Inventory inventory, ItemStack required) {
        return Arrays.stream(inventory.getContents())
            .filter(item -> item != null && item.isSimilar(required))
            .mapToInt(ItemStack::getAmount).sum();
    }

    public void populateChallenges(Map<ChallengeKey, ChallengeCompletion> challengeMap) {
        for (ChallengeId challengeId : catalog.index().challengesById().keySet()) {
            ChallengeKey id = ChallengeKey.of(challengeId.value());
            if (!challengeMap.containsKey(id)) {
                challengeMap.put(id, new ChallengeCompletion(id, null, 0, 0));
            }
        }
    }

    public boolean isResetOnCreate() {
        return runtimeConfigs.current().challenges().resetOnCreate();
    }

    public Collection<ChallengeCompletion> getChallenges(PlayerInfo playerInfo) {
        return completionLogic.getChallenges(playerInfo).values();
    }

    String getBroadcastText() {
        return runtimeConfigs.current().challenges().broadcast().prefix();
    }

    public void completeChallengeForAdmin(@NotNull PlayerInfo target, @NotNull ChallengeKey challengeId,
                                          @NotNull Runnable onSuccess, @NotNull Consumer<Throwable> onError) {
        challengeExecutor.adminComplete(target, challengeId, onSuccess, onError);
    }

    public void completeChallengesForAdmin(@NotNull PlayerInfo target, @NotNull Collection<ChallengeKey> challengeIds,
                                           @NotNull Runnable onSuccess, @NotNull Consumer<Throwable> onError) {
        challengeExecutor.adminCompleteAll(target, challengeIds, onSuccess, onError);
    }

    public void resetChallengeForAdmin(@NotNull PlayerInfo target, @NotNull ChallengeKey challengeId,
                                       @NotNull Runnable onSuccess, @NotNull Consumer<Throwable> onError) {
        challengeExecutor.adminReset(target, challengeId, onSuccess, onError);
    }

    public void resetAllChallengesForAdmin(@NotNull PlayerInfo target,
                                           @NotNull Runnable onSuccess, @NotNull Consumer<Throwable> onError) {
        challengeExecutor.adminResetAll(target, onSuccess, onError);
    }

    public int checkChallenge(PlayerInfo playerInfo, ChallengeKey challengeId) {
        return completionLogic.checkChallenge(playerInfo, challengeId);
    }

    public @Nullable ChallengeCompletion getChallengeCompletion(@NotNull PlayerInfo playerInfo, @NotNull ChallengeKey challengeId) {
        return completionLogic.getChallenge(playerInfo, challengeId);
    }

    public @Nullable ChallengeCompletion getIslandCompletion(@NotNull String islandName, @NotNull ChallengeKey challengeId) {
        Map<ChallengeKey, ChallengeCompletion> challenges = completionLogic.getIslandChallenges(islandName);
        return challenges.get(challengeId);
    }

    public void shutdown() {
        completionLogic.shutdown();
    }

    public long flushCache() {
        return completionLogic.flushCache();
    }

    @EventHandler
    public void onMemberJoinedEvent(MemberJoinedEvent e) {
        if (!(e.getPlayerInfo() instanceof PlayerInfo playerInfo)) {
            return;
        }
        Map<ChallengeKey, ChallengeCompletion> completions = completionLogic.getIslandChallenges(e.getIslandInfo().getName());
        List<String> permissions = new ArrayList<>();
        for (Map.Entry<ChallengeKey, ChallengeCompletion> entry : completions.entrySet()) {
            if (entry.getValue().getTimesCompleted() > 0) {
                // Stored progress may reference challenges removed from challenges.yml.
                getDefinitionById(entry.getKey()).ifPresent(challenge -> {
                    collectPermissionRewards(challenge.firstCompletionReward(), permissions);
                    collectPermissionRewards(challenge.repeatReward(), permissions);
                });
            }
        }
        if (!permissions.isEmpty()) {
            playerInfo.addPermissions(permissions);
        }
    }

    private static void collectPermissionRewards(@NotNull RewardBundle bundle, @NotNull List<String> permissions) {
        for (ChallengeRewards.RewardAction action : bundle.actions()) {
            if (action instanceof ChallengeRewards.PermissionReward permissionReward) {
                permissions.addAll(permissionReward.permissions());
            }
        }
    }
}
