package us.talabrek.ultimateskyblock.challenge;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.po.I18nUtil;
import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.bootstrap.PluginLog;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.api.event.MemberJoinedEvent;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeCatalog;
import us.talabrek.ultimateskyblock.challenge.catalog.bootstrap.ChallengeCatalogLoader;
import us.talabrek.ultimateskyblock.challenge.catalog.bootstrap.ChallengeCatalogRuntimeAdapter;
import us.talabrek.ultimateskyblock.hook.HookManager;
import us.talabrek.ultimateskyblock.player.PerkLogic;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.ComponentLineSplitter;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static dk.lockfuglsang.minecraft.po.I18nUtil.legacyArg;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static dk.lockfuglsang.minecraft.util.FormatUtil.stripFormatting;
import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;
import static us.talabrek.ultimateskyblock.message.Msg.ERROR;
import static us.talabrek.ultimateskyblock.message.Msg.MUTED;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;

/**
 * The home of challenge business logic.
 */
@Singleton
public class ChallengeLogic implements Listener {
    public static final int COLS_PER_ROW = 9;
    public static final int ROWS_OF_RANKS = 5;
    public static final int CHALLENGE_PAGE_SIZE = ROWS_OF_RANKS * COLS_PER_ROW;

    private final Logger logger;
    private final uSkyBlock plugin;
    private final RuntimeConfigs runtimeConfigs;
    private final PerkLogic perkLogic;
    private final HookManager hookManager;

    private final Map<String, Rank> ranks;
    // Fast O(1) lookup for challenges by canonical id
    private final Map<ChallengeKey, Challenge> byId = new HashMap<>();

    public final ChallengeDefaults defaults;
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
        ChallengeCatalog catalog = challengeCatalogLoader.load();
        this.defaults = ChallengeCatalogRuntimeAdapter.legacyDefaults(runtimeConfigs.current().challenges());
        ranks = new ChallengeCatalogRuntimeAdapter().adapt(catalog, runtimeConfigs.current().challenges());
        rebuildIndex();
        completionLogic = new ChallengeCompletionLogic(this, plugin, scheduler, runtimeConfigs, challengeProgressRepository);
        challengeExecutor = new ChallengeExecutor(logger, plugin, scheduler, this, hookManager, perkLogic, completionLogic.progressCache(), challengeProgressRepository);
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void rebuildIndex() {
        byId.clear();
        for (Rank rank : ranks.values()) {
            for (Challenge challenge : rank.getChallenges()) {
                byId.put(challenge.getId(), challenge);
            }
        }
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
        private final @Nullable Challenge challenge;
        private final @NotNull List<String> suggestions;
        private final @NotNull String normalizedInput;

        private ChallengeLookupResult(
            Status status,
            @Nullable Challenge challenge,
            @NotNull List<String> suggestions,
            @NotNull String normalizedInput
        ) {
            this.status = status;
            this.challenge = challenge;
            this.suggestions = List.copyOf(suggestions);
            this.normalizedInput = normalizedInput;
        }

        public static ChallengeLookupResult found(@NotNull Challenge c, @NotNull String normalizedInput) {
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

        public @Nullable Challenge getChallenge() {
            return challenge;
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
     * Fast-path exact lookup by {@link ChallengeKey} id.
     */
    public Optional<Challenge> getChallengeById(@NotNull ChallengeKey key) {
        return Optional.ofNullable(byId.get(key));
    }

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

        // 1) exact internal id
        Optional<Challenge> exactId = getChallengeById(ChallengeKey.of(inputLower));
        if (exactId.isPresent()) {
            return ChallengeLookupResult.found(exactId.get(), inputLower);
        }

        Collection<Challenge> all = List.copyOf(byId.values());

        // 2) exact display name (color-stripped)
        for (Challenge c : all) {
            String display = stripFormatting(c.getDisplayName());
            if (normalizeLower(display).equals(inputLower)) {
                return ChallengeLookupResult.found(c, inputLower);
            }
        }

        // 3) exact slug match – collect candidates
        List<Challenge> candidates = new ArrayList<>();
        for (Challenge c : all) {
            if (toSlug(c.getId().id()).equals(inputSlug) || toSlug(stripFormatting(c.getDisplayName())).equals(inputSlug)) {
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
            for (Challenge c : all) {
                String idSlug = toSlug(c.getId().id());
                String dnSlug = toSlug(stripFormatting(c.getDisplayName()));
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

    private static List<String> toSuggestionList(List<Challenge> candidates, int max) {
        List<String> list = new ArrayList<>();
        for (Challenge c : candidates) {
            list.add(stripFormatting(c.getDisplayName()));
            if (list.size() >= max) break;
        }
        return list;
    }

    public @NotNull List<Rank> getRanks() {
        return List.copyOf(ranks.values());
    }

    public @NotNull List<ChallengeKey> getAvailableChallenges(PlayerInfo playerInfo) {
        List<ChallengeKey> list = new ArrayList<>();
        if (playerInfo == null || !playerInfo.getHasIsland()) {
            return list;
        }
        for (Rank rank : ranks.values()) {
            if (rank.isAvailable(playerInfo)) {
                for (Challenge challenge : rank.getChallenges()) {
                    if (challenge.getMissingRequirements(playerInfo).isEmpty()) {
                        list.add(challenge.getId());
                    }
                }
            }
        }
        return list;
    }

    public @NotNull List<ChallengeKey> getAllChallengeIds() {
        return List.copyOf(byId.keySet());
    }

    public @NotNull List<Challenge> getChallengesForRank(String rank) {
        return ranks.containsKey(rank) ? ranks.get(rank).getChallenges() : Collections.emptyList();
    }

    public void completeChallenge(@NotNull Player player, @NotNull ChallengeKey id) {
        challengeExecutor.attempt(player, id);
    }

    public void completeChallenge(@NotNull Player player, @NotNull ChallengeKey id, @NotNull List<Inventory> itemSources) {
        challengeExecutor.attempt(player, id, itemSources);
    }

    public void whenChallengesLoaded(@Nullable PlayerInfo playerInfo, @NotNull Runnable onLoaded, @NotNull java.util.function.Consumer<Throwable> onError) {
        completionLogic.whenChallengesLoaded(playerInfo, onLoaded, onError);
    }

    public int getCountOf(Inventory inventory, ItemStack required) {
        return Arrays.stream(inventory.getContents())
            .filter(item -> item != null && item.isSimilar(required))
            .mapToInt(ItemStack::getAmount).sum();
    }

    private ItemStack getItemStack(ChallengeCompletion completion, Challenge challenge) {
        ItemStack currentChallengeItem = challenge.getDisplayItem(completion, defaults.enableEconomyPlugin);
        ItemMeta meta = currentChallengeItem.getItemMeta();
        if (meta == null) {
            return currentChallengeItem;
        }
        if (completion.getTimesCompleted() > 0) {
            meta.addEnchant(Enchantment.LOYALTY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        currentChallengeItem.setItemMeta(meta);
        return currentChallengeItem;
    }

    public void populateChallenges(Map<ChallengeKey, ChallengeCompletion> challengeMap) {
        for (Rank rank : ranks.values()) {
            for (Challenge challenge : rank.getChallenges()) {
                ChallengeKey id = challenge.getId();
                if (!challengeMap.containsKey(id)) {
                    challengeMap.put(id, new ChallengeCompletion(id, null, 0, 0));
                }
            }
        }
    }

    public void populateChallengeRank(Inventory menu, PlayerInfo pi, int page) {
        List<Rank> ranksOnPage = new ArrayList<>(ranks.values());
        // page 1 = 0-4, 2 = 5-8, ...
        if (page > 0) {
            ranksOnPage = getRanksForPage(page, ranksOnPage);
        }
        int location = 0;
        for (Rank rank : ranksOnPage) {
            location = populateChallengeRank(menu, rank, location, pi);
            if ((location % 9) != 0) {
                location += (9 - (location % 9)); // Skip the rest of that line
            }
            if (location >= CHALLENGE_PAGE_SIZE) {
                break;
            }
        }
    }

    private List<Rank> getRanksForPage(int page, List<Rank> ranksOnPage) {
        int rowsToSkip = (page - 1) * ROWS_OF_RANKS;
        List<Rank> allRanks = new ArrayList<>(ranksOnPage);

        int i = 1;
        for (Iterator<Rank> it = ranksOnPage.iterator(); it.hasNext(); i++) {
            it.next();
            int rowsInRanks = calculateRows(allRanks.subList(0, i));
            if (rowsToSkip <= 0 || ((rowsToSkip - rowsInRanks) < 0)) {
                return ranksOnPage;
            }
            it.remove();
        }
        return ranksOnPage;
    }

    private int calculateRows(List<Rank> ranksOnPage) {
        int totalRows = 0;
        int previousRowsOnPage = 0;
        int currentRows;

        for (Rank rank : ranksOnPage) {
            currentRows = getRows(rank);
            totalRows += currentRows;

            if (previousRowsOnPage < 5 && (currentRows + previousRowsOnPage) > 5) {
                totalRows = totalRows + (5 - previousRowsOnPage);
                previousRowsOnPage = currentRows;
            } else {
                previousRowsOnPage = previousRowsOnPage + currentRows;
            }
        }
        return totalRows;
    }

    private int getRows(Rank rank) {
        int rankSize = 0;
        for (Challenge challenge : rank.getChallenges()) {
            rankSize += challenge.getOffset() + 1;
        }
        return (int) Math.ceil(rankSize / 8f);
    }

    public int populateChallengeRank(Inventory menu, final Rank rank, int location, final PlayerInfo playerInfo) {
        populateChallengeHeader(rank, location, menu);
        List<String> missingRankRequirements = rank.getMissingRequirements(playerInfo);
        // Tracks if the previous challenge was completed. Relevant for progression through challenge chains, e.g. novice/adept/expert builder.
        boolean wasPreviousChallengeCompleted = false;
        for (Challenge challenge : rank.getChallenges()) {
            if (challenge.getOffset() == -1 && !wasPreviousChallengeCompleted) {
                continue; // skip
            }
            location += challenge.getOffset() + 1;
            if ((location % 9) == 0) {
                location++; // Skip rank-row
            }
            if (location >= CHALLENGE_PAGE_SIZE) {
                break;
            }

            ChallengeCompletion completion = getChallengeCompletion(playerInfo, challenge.getId());
            wasPreviousChallengeCompleted = completion != null && completion.getTimesCompleted() > 0;

            try {
                List<String> missingReqs = challenge.getMissingRequirements(playerInfo);
                boolean challengeLocked = !missingRankRequirements.isEmpty() || !missingReqs.isEmpty();

                ItemStack displayItem;

                if (!challengeLocked) {
                    displayItem = getItemStack(completion, challenge);
                } else {
                    displayItem = renderLockedChallengeItem(challenge, missingReqs, missingRankRequirements);
                }
                menu.setItem(location, displayItem);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Invalid challenge " + challenge, e);
            }
        }
        return location;
    }

    private void populateChallengeHeader(Rank rank, int location, Inventory menu) {
        ItemStack displayItem = rank.getDisplayItem();
        ItemStackUtil.setComponentDisplayName(displayItem,
            tr("Rank: <rank>", Style.style(YELLOW, BOLD), legacyArg("rank", rank.getName())));
        List<Component> lores = new ArrayList<>(
            ComponentLineSplitter.splitLines(tr("Complete most challenges in<newline>this rank to unlock the next rank.", MUTED))
        );
        if (location < (CHALLENGE_PAGE_SIZE / 2)) {
            lores.add(tr("Click here to show the previous page.", PRIMARY));
        } else {
            lores.add(tr("Click here to show the next page.", PRIMARY));
        }
        ItemStackUtil.setComponentLore(displayItem, lores);
        menu.setItem(location, displayItem);
    }

    private ItemStack renderLockedChallengeItem(Challenge challenge, List<String> missingReqs, List<String> missingRankRequirements) {
        ItemStack locked = challenge.getLockedDisplayItem();
        if (locked == null) {
            locked = new ItemStack(challenge.getDisplayItem());
        }

        ItemStackUtil.setComponentDisplayName(locked, tr("Locked Challenge", ERROR));
        List<String> lores = new ArrayList<>();
        if (defaults.showLockedChallengeName) {
            lores.add(challenge.getDisplayName());
        }
        lores.addAll(missingReqs);
        lores.addAll(missingRankRequirements);
        ItemStackUtil.setComponentLore(locked, lores.stream().map(I18nUtil::fromLegacy).toList());
        return locked;
    }

    public boolean isResetOnCreate() {
        return runtimeConfigs.current().challenges().resetOnCreate();
    }

    public int getTotalPages() {
        int totalRows = calculateRows(getRanks());
        return (int) Math.ceil(1f * totalRows / ROWS_OF_RANKS);
    }

    public Collection<ChallengeCompletion> getChallenges(PlayerInfo playerInfo) {
        return completionLogic.getChallenges(playerInfo).values();
    }

    String getBroadcastText() {
        return runtimeConfigs.current().challenges().broadcast().prefix();
    }

    public void completeChallenge(PlayerInfo playerInfo, ChallengeKey challengeId) {
        completionLogic.completeChallenge(playerInfo, challengeId);
    }

    public void resetChallenge(PlayerInfo playerInfo, ChallengeKey challengeId) {
        completionLogic.resetChallenge(playerInfo, challengeId);
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

    public void resetAllChallenges(PlayerInfo playerInfo) {
        completionLogic.resetAllChallenges(playerInfo);
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
                Challenge challenge = getChallengeById(entry.getKey()).orElse(null);
                if (challenge != null && challenge.getReward().getPermissionReward() != null) {
                    permissions.addAll(Arrays.asList(challenge.getReward().getPermissionReward().split(" ")));
                }
            }
        }
        if (!permissions.isEmpty()) {
            playerInfo.addPermissions(permissions);
        }
    }
}
