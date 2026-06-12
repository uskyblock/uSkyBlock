package us.talabrek.ultimateskyblock.challenge;

import dk.lockfuglsang.minecraft.util.BlockRequirement;
import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.api.event.ChallengeRankUnlockedEvent;
import us.talabrek.ultimateskyblock.block.BlockCollection;
import us.talabrek.ultimateskyblock.challenge.ChallengeUnlockEvaluator.MissingRequirement;
import us.talabrek.ultimateskyblock.challenge.ChallengeUnlockEvaluator.UnlockContext;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.BlockRequirementSpec;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.CompletionRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.EntityPresenceRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.EntityRequirementSpec;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.InventoryItemsRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.IslandBlocksRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.IslandLevelRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.ItemRequirementSpec;
import us.talabrek.ultimateskyblock.challenge.catalog.RankId;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.island.IslandKey;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static dk.lockfuglsang.minecraft.po.I18nUtil.parseMini;
import static us.talabrek.ultimateskyblock.message.Msg.ERROR;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Msg.send;
import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.message.Msg.sendTr;
import static us.talabrek.ultimateskyblock.message.Placeholder.component;
import static us.talabrek.ultimateskyblock.message.Placeholder.number;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;

/**
 * The only write path for challenge progress. Evaluates catalog completion requirements, holds the
 * per-island completion lock, and persists asynchronously before any reward is handed out.
 */
public final class ChallengeExecutor {
    private final Logger logger;
    private final uSkyBlock plugin;
    private final Scheduler scheduler;
    private final ChallengeLogic challengeLogic;
    private final ChallengeUnlockEvaluator unlockEvaluator;
    private final RewardApplier rewardApplier;
    private final ChallengeProgressCache progressCache;
    private final ChallengeProgressRepository repository;

    public ChallengeExecutor(
        @NotNull Logger logger,
        @NotNull uSkyBlock plugin,
        @NotNull Scheduler scheduler,
        @NotNull ChallengeLogic challengeLogic,
        @NotNull ChallengeUnlockEvaluator unlockEvaluator,
        @NotNull RewardApplier rewardApplier,
        @NotNull ChallengeProgressCache progressCache,
        @NotNull ChallengeProgressRepository repository
    ) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.challengeLogic = Objects.requireNonNull(challengeLogic, "challengeLogic");
        this.unlockEvaluator = Objects.requireNonNull(unlockEvaluator, "unlockEvaluator");
        this.rewardApplier = Objects.requireNonNull(rewardApplier, "rewardApplier");
        this.progressCache = Objects.requireNonNull(progressCache, "progressCache");
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public void attempt(@NotNull Player player, @NotNull ChallengeKey challengeId) {
        attempt(player, challengeId, List.of(player.getInventory()));
    }

    public void attempt(@NotNull Player player, @NotNull ChallengeKey challengeId, @NotNull List<Inventory> itemSources) {
        attempt(player, challengeId, itemSources, () -> {
        });
    }

    /**
     * @param onSettled runs on the main thread once the attempt has terminally settled
     *                  (success, failure, or rejection) - in-memory state is current by then.
     */
    public void attempt(@NotNull Player player, @NotNull ChallengeKey challengeId, @NotNull List<Inventory> itemSources, @NotNull Runnable onSettled) {
        PlayerInfo playerInfo = plugin.getPlayerInfo(player);
        if (playerInfo == null || !playerInfo.getHasIsland() || playerInfo.locationForParty() == null) {
            sendErrorTr(player, "You can only submit challenges when you have an island!");
            return;
        }

        Optional<ChallengeDefinition> definition = challengeLogic.getDefinitionById(challengeId);
        if (definition.isEmpty()) {
            sendErrorTr(player, "No challenge with id <challenge-id> found", unparsed("challenge-id", challengeId.id()));
            return;
        }

        if (!plugin.playerIsOnOwnIsland(player)) {
            sendErrorTr(player, "You must be on your island to do that!");
            return;
        }

        IslandKey islandKey = IslandKey.fromIslandName(playerInfo.locationForParty());
        LoadedChallengeProgress loaded = progressCache.getIfLoaded(islandKey).orElse(null);
        if (loaded != null) {
            attemptLoaded(player, playerInfo, definition.get(), itemSources, loaded, onSettled);
            return;
        }

        sendTr(player, "Loading challenge data...");
        progressCache.loadAsync(islandKey).whenComplete((state, error) -> scheduler.sync(() -> {
            if (error != null) {
                logger.log(Level.WARNING, "Unable to load challenge progress for " + islandKey.value(), error);
                sendErrorTr(player, "Unable to load challenge progress right now. Please try again.");
                onSettled.run();
                return;
            }
            attemptLoaded(player, playerInfo, definition.get(), itemSources, state, onSettled);
        }));
    }

    private void attemptLoaded(
        @NotNull Player player,
        @NotNull PlayerInfo playerInfo,
        @NotNull ChallengeDefinition challenge,
        @NotNull List<Inventory> itemSources,
        @NotNull LoadedChallengeProgress loaded,
        @NotNull Runnable onSettled
    ) {
        if (loaded.isWriteLocked()) {
            sendErrorTr(player, "Challenge progress is temporarily locked due to a persistence error. Please contact an administrator.");
            onSettled.run();
            return;
        }
        if (!loaded.tryBeginCompletion()) {
            sendErrorTr(player, "Challenge completion is already in progress for your island.");
            onSettled.run();
            return;
        }

        try {
            Map<ChallengeKey, ChallengeCompletion> progress = loaded.snapshot();
            ChallengeKey challengeKey = ChallengeKey.of(challenge.id().value());
            ChallengeCompletion completion = progress.computeIfAbsent(challengeKey, id -> new ChallengeCompletion(id, null, 0, 0));

            UnlockContext context = new UnlockContext(progress, player::hasPermission, islandLevel(player));
            List<MissingRequirement> missingUnlocks = unlockEvaluator.missingChallengeRequirements(challenge, context);
            if (!missingUnlocks.isEmpty()) {
                sendErrorTr(player, "The <challenge> challenge is not available yet!",
                    component("challenge", ChallengeText.displayName(challenge)));
                unlockEvaluator.describe(missingUnlocks).forEach(line -> send(player, line));
                loaded.finishCompletion();
                onSettled.run();
                return;
            }
            if (completion.getTimesCompleted() > 0
                && (!challenge.repeatPolicy().repeatable() || !hasInventoryHandIn(challenge))) {
                sendErrorTr(player, "The <challenge> challenge is not repeatable!",
                    component("challenge", ChallengeText.displayName(challenge)));
                loaded.finishCompletion();
                onSettled.run();
                return;
            }
            if (completion.isOnCooldown() && !challenge.repeatPolicy().isUnlimited()
                && completion.getTimesCompletedInCooldown() >= challenge.repeatPolicy().repeatLimit()) {
                sendErrorTr(player, "You cannot complete the <challenge> challenge again yet!",
                    component("challenge", ChallengeText.displayName(challenge)));
                loaded.finishCompletion();
                onSettled.run();
                return;
            }

            sendTr(player, "Trying to complete challenge <challenge>.",
                component("challenge", ChallengeText.displayName(challenge), PRIMARY));

            RequirementCheck check = checkAndConsumeRequirements(player, challenge, completion, itemSources);
            if (!check.completed()) {
                loaded.finishCompletion();
                onSettled.run();
                return;
            }

            boolean isFirstCompletion = completion.getTimesCompleted() == 0;
            Set<RankId> previouslyUnlocked = unlockEvaluator.unlockedRanks(context);
            applyCompletion(progress, challengeKey, challenge, completion);
            Set<RankId> newlyUnlocked = unlockEvaluator.unlockedRanks(
                new UnlockContext(progress, player::hasPermission, context.islandLevel()));
            newlyUnlocked.removeAll(previouslyUnlocked);
            persistCompletion(player, playerInfo, challenge, loaded, progress, isFirstCompletion, check.consumedItems(), newlyUnlocked, onSettled);
        } catch (Throwable t) {
            loaded.finishCompletion();
            logger.log(Level.WARNING, "Failed to complete challenge " + challenge.id().value() + " for " + player.getName(), t);
            sendErrorTr(player, "Challenge completion failed unexpectedly.");
            onSettled.run();
        }
    }

    /**
     * Checks every completion requirement, reporting all shortfalls, and only consumes hand-in
     * items once everything else has passed.
     */
    private @NotNull RequirementCheck checkAndConsumeRequirements(
        @NotNull Player player,
        @NotNull ChallengeDefinition challenge,
        @NotNull ChallengeCompletion completion,
        @NotNull List<Inventory> itemSources
    ) {
        boolean fulfilled = true;
        Map<ItemStack, Integer> requiredItems = new LinkedHashMap<>();
        for (CompletionRequirement requirement : challenge.completionRequirements()) {
            switch (requirement) {
                case IslandBlocksRequirement blocks -> fulfilled &= checkIslandBlocks(player, blocks);
                case EntityPresenceRequirement entities -> fulfilled &= checkEntities(player, entities);
                case IslandLevelRequirement level -> fulfilled &= checkIslandLevel(player, level);
                case InventoryItemsRequirement items ->
                    collectRequiredItems(items, completion.getTimesCompletedInCooldown(), requiredItems);
            }
        }
        if (!requiredItems.isEmpty()) {
            fulfilled &= hasAllItems(player, requiredItems, itemSources);
        }
        if (!fulfilled) {
            return new RequirementCheck(false, Map.of());
        }
        if (!requiredItems.isEmpty() && challenge.properties().consumeItemsOnCompletion()) {
            removeRequiredItems(itemSources, requiredItems);
            return new RequirementCheck(true, requiredItems);
        }
        return new RequirementCheck(true, Map.of());
    }

    /**
     * The in-memory state is only updated and rewards are only handed out once the completion is
     * durable; on a persistence failure the consumed items are returned and memory still matches
     * the database.
     */
    private void persistCompletion(
        @NotNull Player player,
        @NotNull PlayerInfo playerInfo,
        @NotNull ChallengeDefinition challenge,
        @NotNull LoadedChallengeProgress loaded,
        @NotNull Map<ChallengeKey, ChallengeCompletion> progress,
        boolean isFirstCompletion,
        @NotNull Map<ItemStack, Integer> consumedItems,
        @NotNull Set<RankId> newlyUnlockedRanks,
        @NotNull Runnable onSettled
    ) {
        Map<ChallengeKey, ChallengeCompletion> snapshot = LoadedChallengeProgress.copyProgress(progress);
        scheduler.async(() -> {
            try {
                repository.replace(loaded.islandKey(), snapshot);
                scheduler.sync(() -> {
                    loaded.replace(progress);
                    loaded.finishCompletion();
                    if (player.isOnline()) {
                        rewardApplier.apply(player, playerInfo, challenge, isFirstCompletion);
                    } else {
                        logger.warning("Player " + player.getName() + " disconnected before the reward for challenge "
                            + challenge.id().value() + " could be handed out; the completion is recorded.");
                    }
                    for (RankId rankId : newlyUnlockedRanks) {
                        plugin.getServer().getPluginManager()
                            .callEvent(new ChallengeRankUnlockedEvent(loaded.islandKey().value(), rankId.value()));
                    }
                    onSettled.run();
                });
            } catch (Throwable t) {
                scheduler.sync(() -> {
                    loaded.lockWrites();
                    logger.log(Level.WARNING, "Failed to persist challenge progress for island " + loaded.islandKey().value(), t);
                    refundItems(player, consumedItems);
                    sendErrorTr(player, "Challenge progress could not be saved. Challenge completions are locked for this island until the cache is flushed or the server restarts.");
                    onSettled.run();
                });
            }
        });
    }

    private void refundItems(@NotNull Player player, @NotNull Map<ItemStack, Integer> consumedItems) {
        if (consumedItems.isEmpty()) {
            return;
        }
        ItemStack[] items = ItemStackUtil.asValidItemStacksWithAmount(consumedItems);
        if (!player.isOnline()) {
            logger.warning("Unable to return challenge hand-in items to offline player " + player.getName()
                + ": " + Arrays.toString(items));
            return;
        }
        HashMap<Integer, ItemStack> leftOvers = player.getInventory().addItem(items);
        for (ItemStack item : leftOvers.values()) {
            player.getWorld().dropItem(player.getLocation(), item);
        }
        sendTr(player, "Your handed-in items were returned.");
    }

    public void adminComplete(
        @NotNull PlayerInfo target,
        @NotNull ChallengeKey challengeId,
        @NotNull Runnable onSuccess,
        @NotNull Consumer<Throwable> onError
    ) {
        adminCompleteAll(target, List.of(challengeId), onSuccess, onError);
    }

    /**
     * Completes all given challenges in one lock acquisition and one persist; completing them
     * one by one would fail on the island's in-flight lock from the second challenge onward.
     */
    public void adminCompleteAll(
        @NotNull PlayerInfo target,
        @NotNull Collection<ChallengeKey> challengeIds,
        @NotNull Runnable onSuccess,
        @NotNull Consumer<Throwable> onError
    ) {
        Map<ChallengeKey, ChallengeDefinition> challenges = new LinkedHashMap<>();
        for (ChallengeKey challengeId : challengeIds) {
            Optional<ChallengeDefinition> challenge = challengeLogic.getDefinitionById(challengeId);
            if (challenge.isEmpty()) {
                onError.accept(new IllegalArgumentException("Unknown challenge " + challengeId.id()));
                return;
            }
            challenges.put(challengeId, challenge.get());
        }
        mutateProgress(target, progress -> {
            for (Map.Entry<ChallengeKey, ChallengeDefinition> entry : challenges.entrySet()) {
                ChallengeCompletion completion = progress.computeIfAbsent(entry.getKey(), id -> new ChallengeCompletion(id, null, 0, 0));
                if (!completion.isOnCooldown()) {
                    Duration resetWindow = entry.getValue().repeatPolicy().resetWindow();
                    completion.setCooldownUntil(resetWindow.isPositive() ? Instant.now().plus(resetWindow) : null);
                }
                completion.addTimesCompleted();
            }
        }, onSuccess, onError);
    }

    public void adminReset(
        @NotNull PlayerInfo target,
        @NotNull ChallengeKey challengeId,
        @NotNull Runnable onSuccess,
        @NotNull Consumer<Throwable> onError
    ) {
        mutateProgress(target, progress -> {
            ChallengeCompletion completion = progress.computeIfAbsent(challengeId, id -> new ChallengeCompletion(id, null, 0, 0));
            completion.setTimesCompleted(0);
            completion.setCooldownUntil(null);
        }, onSuccess, onError);
    }

    public void adminResetAll(
        @NotNull PlayerInfo target,
        @NotNull Runnable onSuccess,
        @NotNull Consumer<Throwable> onError
    ) {
        mutateProgress(target, progress -> {
            progress.clear();
            challengeLogic.populateChallenges(progress);
        }, onSuccess, onError);
    }

    /**
     * Administrative mutations share the player completion flow's island lock and async
     * persistence so they cannot race a completion's write-behind and silently lose either update.
     */
    private void mutateProgress(
        @NotNull PlayerInfo target,
        @NotNull Consumer<Map<ChallengeKey, ChallengeCompletion>> mutation,
        @NotNull Runnable onSuccess,
        @NotNull Consumer<Throwable> onError
    ) {
        if (!target.getHasIsland() || target.locationForParty() == null) {
            onError.accept(new IllegalStateException("Player has no island"));
            return;
        }
        IslandKey islandKey = IslandKey.fromIslandName(target.locationForParty());
        LoadedChallengeProgress existing = progressCache.getIfLoaded(islandKey).orElse(null);
        if (existing != null) {
            mutateLoaded(existing, mutation, onSuccess, onError);
            return;
        }
        progressCache.loadAsync(islandKey).whenComplete((loaded, error) -> scheduler.sync(() -> {
            if (error != null) {
                onError.accept(error);
                return;
            }
            mutateLoaded(loaded, mutation, onSuccess, onError);
        }));
    }

    private void mutateLoaded(
        @NotNull LoadedChallengeProgress loaded,
        @NotNull Consumer<Map<ChallengeKey, ChallengeCompletion>> mutation,
        @NotNull Runnable onSuccess,
        @NotNull Consumer<Throwable> onError
    ) {
        if (loaded.isWriteLocked()) {
            onError.accept(new IllegalStateException("Challenge progress for island " + loaded.islandKey().value()
                + " is locked after a persistence error; flush the cache or restart the server"));
            return;
        }
        if (!loaded.tryBeginCompletion()) {
            onError.accept(new IllegalStateException("A challenge completion is already in progress for island "
                + loaded.islandKey().value()));
            return;
        }
        Map<ChallengeKey, ChallengeCompletion> progress = loaded.snapshot();
        try {
            mutation.accept(progress);
        } catch (Throwable t) {
            loaded.finishCompletion();
            onError.accept(t);
            return;
        }
        Map<ChallengeKey, ChallengeCompletion> snapshot = LoadedChallengeProgress.copyProgress(progress);
        scheduler.async(() -> {
            try {
                repository.replace(loaded.islandKey(), snapshot);
                scheduler.sync(() -> {
                    loaded.replace(progress);
                    loaded.finishCompletion();
                    onSuccess.run();
                });
            } catch (Throwable t) {
                scheduler.sync(() -> {
                    loaded.lockWrites();
                    logger.log(Level.WARNING, "Failed to persist challenge progress for island " + loaded.islandKey().value(), t);
                    onError.accept(t);
                });
            }
        });
    }

    private void applyCompletion(
        @NotNull Map<ChallengeKey, ChallengeCompletion> progress,
        @NotNull ChallengeKey challengeKey,
        @NotNull ChallengeDefinition challenge,
        @NotNull ChallengeCompletion completion
    ) {
        if (!completion.isOnCooldown()) {
            Duration resetWindow = challenge.repeatPolicy().resetWindow();
            if (resetWindow.isPositive()) {
                completion.setCooldownUntil(Instant.now().plus(resetWindow));
            } else {
                completion.setCooldownUntil(null);
            }
        }
        completion.addTimesCompleted();
        progress.put(challengeKey, completion);
    }

    private double islandLevel(@NotNull Player player) {
        IslandInfo islandInfo = plugin.getIslandInfo(player);
        return islandInfo != null ? islandInfo.getLevel() : 0d;
    }

    private static boolean hasInventoryHandIn(@NotNull ChallengeDefinition challenge) {
        return challenge.completionRequirements().stream().anyMatch(InventoryItemsRequirement.class::isInstance);
    }

    private boolean checkIslandLevel(@NotNull Player player, @NotNull IslandLevelRequirement requirement) {
        if (islandLevel(player) >= requirement.minimumLevel()) {
            return true;
        }
        sendErrorTr(player, "Your island must be level <level> to complete this challenge!",
            number("level", requirement.minimumLevel()));
        return false;
    }

    private boolean checkIslandBlocks(@NotNull Player player, @NotNull IslandBlocksRequirement requirement) {
        List<BlockRequirement> requiredBlocks = new ArrayList<>();
        for (BlockRequirementSpec spec : requirement.blocks()) {
            requiredBlocks.add(new BlockRequirement(spec.prototype(), spec.amount()));
        }
        if (islandContains(player, requiredBlocks, requirement.radius())) {
            return true;
        }
        sendErrorTr(player, "You must be standing within <radius> blocks of all required items.",
            number("radius", requirement.radius()));
        return false;
    }

    private boolean checkEntities(@NotNull Player player, @NotNull EntityPresenceRequirement requirement) {
        List<EntityMatch> requiredEntities = new ArrayList<>();
        for (EntityRequirementSpec spec : requirement.entities()) {
            requiredEntities.add(new EntityMatch(spec.type(), spec.metadata(), spec.count()));
        }
        return hasEntitiesNear(player, requiredEntities, requirement.radius());
    }

    private boolean islandContains(@NotNull Player player, @NotNull List<BlockRequirement> itemStacks, int radius) {
        final Location location = player.getLocation();
        final int px = location.getBlockX();
        final int py = location.getBlockY();
        final int pz = location.getBlockZ();
        World world = Objects.requireNonNull(location.getWorld());
        BlockCollection blockCollection = new BlockCollection();
        for (int x = px - radius; x <= px + radius; x++) {
            for (int y = py - radius; y <= py + radius; y++) {
                for (int z = pz - radius; z <= pz + radius; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    blockCollection.add(block);
                }
            }
        }
        Component diff = blockCollection.diff(itemStacks);
        if (diff != null) {
            send(player, diff);
            return false;
        }
        return true;
    }

    private boolean hasEntitiesNear(@NotNull Player player, @NotNull List<EntityMatch> requiredEntities, int radius) {
        Map<EntityMatch, Integer> countMap = new LinkedHashMap<>();
        Map<EntityType, Set<EntityMatch>> matchMap = new EnumMap<>(EntityType.class);
        for (EntityMatch match : requiredEntities) {
            countMap.put(match, match.getCount());
            Set<EntityMatch> set = matchMap.computeIfAbsent(match.getType(), type -> new HashSet<>());
            set.add(match);
        }
        List<Entity> nearbyEntities = player.getNearbyEntities(radius, radius, radius);
        for (Entity entity : nearbyEntities) {
            if (matchMap.containsKey(entity.getType())) {
                for (Iterator<EntityMatch> iterator = matchMap.get(entity.getType()).iterator(); iterator.hasNext(); ) {
                    EntityMatch match = iterator.next();
                    if (match.matches(entity)) {
                        int newCount = countMap.get(match) - 1;
                        if (newCount <= 0) {
                            countMap.remove(match);
                            iterator.remove();
                        } else {
                            countMap.put(match, newCount);
                        }
                    }
                }
            }
        }
        if (!countMap.isEmpty()) {
            sendTr(player, "Still missing the following entities:");
            for (Map.Entry<EntityMatch, Integer> entry : countMap.entrySet()) {
                send(player, parseMini(
                    "<muted> - <count> x <entity>",
                    number("count", entry.getValue(), ERROR),
                    component("entity", entry.getKey().getDisplayName().applyFallbackStyle(PRIMARY))
                ));
            }
        }
        return countMap.isEmpty();
    }

    private void collectRequiredItems(
        @NotNull InventoryItemsRequirement requirement,
        int timesCompletedInCooldown,
        @NotNull Map<ItemStack, Integer> requiredItems
    ) {
        for (ItemRequirementSpec spec : requirement.items()) {
            requiredItems.put(spec.item().create(), spec.amountForRepetitions(timesCompletedInCooldown));
        }
    }

    private boolean hasAllItems(
        @NotNull Player player,
        @NotNull Map<ItemStack, Integer> requiredItems,
        @NotNull List<Inventory> itemSources
    ) {
        Component missingItems = Component.empty();
        boolean hasAll = true;
        for (Map.Entry<ItemStack, Integer> required : requiredItems.entrySet()) {
            ItemStack requiredType = required.getKey();
            int requiredAmount = required.getValue();
            int available = countOf(itemSources, requiredType);
            if (available < requiredAmount) {
                Component name = ItemStackUtil.getItemName(requiredType);
                missingItems = missingItems.append(parseMini(" <count> <item>",
                    number("count", requiredAmount - available, ERROR),
                    component("item", name, PRIMARY)));
                hasAll = false;
            }
        }
        if (!hasAll) {
            sendTr(player, "You are still missing the following items:<items>", component("items", missingItems));
        }
        return hasAll;
    }

    private int countOf(@NotNull Collection<Inventory> inventories, @NotNull ItemStack required) {
        int total = 0;
        for (Inventory inventory : inventories) {
            total += Arrays.stream(inventory.getContents())
                .filter(item -> item != null && item.isSimilar(required))
                .mapToInt(ItemStack::getAmount)
                .sum();
        }
        return total;
    }

    private void removeRequiredItems(@NotNull List<Inventory> inventories, @NotNull Map<ItemStack, Integer> requiredItems) {
        for (Map.Entry<ItemStack, Integer> required : requiredItems.entrySet()) {
            int remaining = required.getValue();
            for (Inventory inventory : inventories) {
                if (remaining <= 0) {
                    break;
                }
                for (ItemStack item : inventory.getContents()) {
                    if (item == null || !item.isSimilar(required.getKey())) {
                        continue;
                    }
                    int removed = Math.min(item.getAmount(), remaining);
                    item.setAmount(item.getAmount() - removed);
                    remaining -= removed;
                    if (item.getAmount() <= 0) {
                        inventory.remove(item);
                    }
                    if (remaining <= 0) {
                        break;
                    }
                }
            }
            if (remaining > 0) {
                throw new IllegalStateException("Could not remove required items for challenge hand-in");
            }
        }
    }

    private record RequirementCheck(boolean completed, @NotNull Map<ItemStack, Integer> consumedItems) {
    }
}
