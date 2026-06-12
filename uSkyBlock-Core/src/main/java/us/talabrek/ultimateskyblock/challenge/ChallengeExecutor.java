package us.talabrek.ultimateskyblock.challenge;

import dk.lockfuglsang.minecraft.util.BlockRequirement;
import dk.lockfuglsang.minecraft.util.FormatUtil;
import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.block.BlockCollection;
import us.talabrek.ultimateskyblock.hook.HookManager;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.island.IslandKey;
import us.talabrek.ultimateskyblock.message.Placeholder;
import us.talabrek.ultimateskyblock.player.Perk;
import us.talabrek.ultimateskyblock.player.PerkLogic;
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
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import static dk.lockfuglsang.minecraft.po.I18nUtil.legacyArg;
import static dk.lockfuglsang.minecraft.po.I18nUtil.parseMini;
import static dk.lockfuglsang.minecraft.po.I18nUtil.trLegacy;
import static us.talabrek.ultimateskyblock.message.Msg.ERROR;
import static us.talabrek.ultimateskyblock.message.Msg.MUTED;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Msg.SECONDARY;
import static us.talabrek.ultimateskyblock.message.Msg.send;
import static us.talabrek.ultimateskyblock.message.Msg.sendError;
import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.message.Msg.sendTr;
import static us.talabrek.ultimateskyblock.message.Placeholder.component;
import static us.talabrek.ultimateskyblock.message.Placeholder.number;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;

public final class ChallengeExecutor {
    private final Logger logger;
    private final uSkyBlock plugin;
    private final Scheduler scheduler;
    private final ChallengeLogic challengeLogic;
    private final ChallengeDefaults defaults;
    private final HookManager hookManager;
    private final PerkLogic perkLogic;
    private final ChallengeProgressCache progressCache;
    private final ChallengeProgressRepository repository;

    public ChallengeExecutor(
        @NotNull Logger logger,
        @NotNull uSkyBlock plugin,
        @NotNull Scheduler scheduler,
        @NotNull ChallengeLogic challengeLogic,
        @NotNull ChallengeDefaults defaults,
        @NotNull HookManager hookManager,
        @NotNull PerkLogic perkLogic,
        @NotNull ChallengeProgressCache progressCache,
        @NotNull ChallengeProgressRepository repository
    ) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.challengeLogic = Objects.requireNonNull(challengeLogic, "challengeLogic");
        this.defaults = Objects.requireNonNull(defaults, "defaults");
        this.hookManager = Objects.requireNonNull(hookManager, "hookManager");
        this.perkLogic = Objects.requireNonNull(perkLogic, "perkLogic");
        this.progressCache = Objects.requireNonNull(progressCache, "progressCache");
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public void attempt(@NotNull Player player, @NotNull ChallengeKey challengeId) {
        attempt(player, challengeId, List.of(player.getInventory()));
    }

    public void attempt(@NotNull Player player, @NotNull ChallengeKey challengeId, @NotNull List<Inventory> itemSources) {
        PlayerInfo playerInfo = plugin.getPlayerInfo(player);
        if (playerInfo == null || !playerInfo.getHasIsland() || playerInfo.locationForParty() == null) {
            sendErrorTr(player, "You can only submit challenges when you have an island!");
            return;
        }

        Optional<Challenge> opt = challengeLogic.getChallengeById(challengeId);
        if (opt.isEmpty()) {
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
            attemptLoaded(player, playerInfo, opt.get(), itemSources, loaded);
            return;
        }

        sendTr(player, "Loading challenge data...");
        progressCache.loadAsync(islandKey).whenComplete((state, error) -> scheduler.sync(() -> {
            if (error != null) {
                logger.log(Level.WARNING, "Unable to load challenge progress for " + islandKey.value(), error);
                sendErrorTr(player, "Unable to load challenge progress right now. Please try again.");
                return;
            }
            attemptLoaded(player, playerInfo, opt.get(), itemSources, state);
        }));
    }

    private void attemptLoaded(
        @NotNull Player player,
        @NotNull PlayerInfo playerInfo,
        @NotNull Challenge challenge,
        @NotNull List<Inventory> itemSources,
        @NotNull LoadedChallengeProgress loaded
    ) {
        if (loaded.isWriteLocked()) {
            sendErrorTr(player, "Challenge progress is temporarily locked due to a persistence error. Please contact an administrator.");
            return;
        }
        if (!loaded.tryBeginCompletion()) {
            sendErrorTr(player, "Challenge completion is already in progress for your island.");
            return;
        }

        try {
            Map<ChallengeKey, ChallengeCompletion> progress = loaded.snapshot();
            ChallengeCompletion completion = progress.computeIfAbsent(challenge.getId(), id -> new ChallengeCompletion(id, null, 0, 0));
            if (!challenge.getRank().isAvailable(playerInfo)) {
                sendErrorTr(player, "The <challenge> challenge is not available yet!",
                    legacyArg("challenge", challenge.getDisplayName()));
                loaded.finishCompletion();
                return;
            }
            if (completion.getTimesCompleted() > 0 && (!challenge.isRepeatable() || challenge.getType() == Challenge.Type.ISLAND)) {
                sendErrorTr(player, "The <challenge> challenge is not repeatable!", legacyArg("challenge", challenge.getDisplayName()));
                loaded.finishCompletion();
                return;
            }
            if (completion.isOnCooldown() && completion.getTimesCompletedInCooldown() >= challenge.getRepeatLimit() && challenge.getRepeatLimit() > 0) {
                sendErrorTr(player, "You cannot complete the <challenge> challenge again yet!",
                    legacyArg("challenge", challenge.getDisplayName()));
                loaded.finishCompletion();
                return;
            }

            sendTr(player, "Trying to complete challenge <challenge>.",
                Placeholder.legacy("challenge", challenge.getDisplayName(), PRIMARY));

            RequirementCheck check = switch (challenge.getType()) {
                case PLAYER -> tryCompleteOnPlayer(player, challenge, completion, itemSources);
                case ISLAND -> new RequirementCheck(tryCompleteOnIsland(player, challenge), Map.of());
                case ISLAND_LEVEL -> new RequirementCheck(tryCompleteIslandLevel(player, challenge), Map.of());
            };
            if (!check.completed()) {
                loaded.finishCompletion();
                return;
            }

            boolean isFirstCompletion = completion.getTimesCompleted() == 0;
            applyCompletion(progress, challenge, completion);
            persistCompletion(player, playerInfo, challenge, loaded, progress, isFirstCompletion, check.consumedItems());
        } catch (Throwable t) {
            loaded.finishCompletion();
            logger.log(Level.WARNING, "Failed to complete challenge " + challenge.getId().id() + " for " + player.getName(), t);
            sendErrorTr(player, "Challenge completion failed unexpectedly.");
        }
    }

    /**
     * The in-memory state is only updated and rewards are only handed out once the completion is
     * durable; on a persistence failure the consumed items are returned and memory still matches
     * the database.
     */
    private void persistCompletion(
        @NotNull Player player,
        @NotNull PlayerInfo playerInfo,
        @NotNull Challenge challenge,
        @NotNull LoadedChallengeProgress loaded,
        @NotNull Map<ChallengeKey, ChallengeCompletion> progress,
        boolean isFirstCompletion,
        @NotNull Map<ItemStack, Integer> consumedItems
    ) {
        Map<ChallengeKey, ChallengeCompletion> snapshot = LoadedChallengeProgress.copyProgress(progress);
        scheduler.async(() -> {
            try {
                repository.replace(loaded.islandKey(), snapshot);
                scheduler.sync(() -> {
                    loaded.replace(progress);
                    loaded.finishCompletion();
                    applyReward(player, playerInfo, challenge, isFirstCompletion);
                });
            } catch (Throwable t) {
                scheduler.sync(() -> {
                    loaded.lockWrites();
                    logger.log(Level.WARNING, "Failed to persist challenge progress for island " + loaded.islandKey().value(), t);
                    refundItems(player, consumedItems);
                    sendErrorTr(player, "Challenge progress could not be saved. Challenge completions are locked for this island until the cache is flushed or the server restarts.");
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
        Optional<Challenge> challenge = challengeLogic.getChallengeById(challengeId);
        if (challenge.isEmpty()) {
            onError.accept(new IllegalArgumentException("Unknown challenge " + challengeId.id()));
            return;
        }
        mutateProgress(target, progress -> {
            ChallengeCompletion completion = progress.computeIfAbsent(challengeId, id -> new ChallengeCompletion(id, null, 0, 0));
            if (!completion.isOnCooldown()) {
                Duration resetDuration = challenge.get().getResetDuration();
                completion.setCooldownUntil(resetDuration.isPositive() ? Instant.now().plus(resetDuration) : null);
            }
            completion.addTimesCompleted();
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

    private record RequirementCheck(boolean completed, @NotNull Map<ItemStack, Integer> consumedItems) {
    }

    private void applyCompletion(
        @NotNull Map<ChallengeKey, ChallengeCompletion> progress,
        @NotNull Challenge challenge,
        @NotNull ChallengeCompletion completion
    ) {
        if (!completion.isOnCooldown()) {
            Duration resetDuration = challenge.getResetDuration();
            if (resetDuration.isPositive()) {
                completion.setCooldownUntil(Instant.now().plus(resetDuration));
            } else {
                completion.setCooldownUntil(null);
            }
        }
        completion.addTimesCompleted();
        progress.put(challenge.getId(), completion);
    }

    private boolean tryCompleteIslandLevel(@NotNull Player player, @NotNull Challenge challenge) {
        return plugin.getIslandInfo(player).getLevel() >= challenge.getRequiredLevel();
    }

    private boolean tryCompleteOnIsland(@NotNull Player player, @NotNull Challenge challenge) {
        List<BlockRequirement> requiredBlocks = challenge.getRequiredBlocks();
        int radius = challenge.getRadius();
        if (islandContains(player, requiredBlocks, radius) && hasEntitiesNear(player, challenge.getRequiredEntities(), radius)) {
            return true;
        }
        sendError(player, dk.lockfuglsang.minecraft.po.I18nUtil.fromLegacy(challenge.getDescription()));
        sendErrorTr(player, "You must be standing within <radius> blocks of all required items.",
            number("radius", challenge.getRadius()));
        return false;
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

    private @NotNull RequirementCheck tryCompleteOnPlayer(
        @NotNull Player player,
        @NotNull Challenge challenge,
        @NotNull ChallengeCompletion completion,
        @NotNull List<Inventory> itemSources
    ) {
        Component missingItems = Component.empty();
        boolean hasAll = true;
        Map<ItemStack, Integer> requiredItems = challenge.getRequiredItems(completion.getTimesCompletedInCooldown());
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
            return new RequirementCheck(false, Map.of());
        }
        if (challenge.isTakeItems()) {
            removeRequiredItems(itemSources, requiredItems);
            return new RequirementCheck(true, requiredItems);
        }
        return new RequirementCheck(true, Map.of());
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

    private void applyReward(
        @NotNull Player player,
        @NotNull PlayerInfo playerInfo,
        @NotNull Challenge challenge,
        boolean isFirstCompletion
    ) {
        sendTr(player, "You completed the <challenge> challenge!",
            Placeholder.legacy("challenge", challenge.getDisplayName(), PRIMARY));
        Reward reward = isFirstCompletion ? challenge.getReward() : challenge.getRepeatReward();
        player.giveExp(reward.getXpReward());

        boolean wasBroadcast = false;
        if (defaults.broadcastCompletion && isFirstCompletion) {
            Bukkit.getServer().broadcastMessage(FormatUtil.normalize(challengeLogic.getBroadcastText()) +
                trLegacy("<player> has completed the <challenge> challenge!",
                    unparsed("player", player.getName(), PRIMARY),
                    Placeholder.legacy("challenge", challenge.getDisplayName(), PRIMARY)));
            wasBroadcast = true;
        }

        sendTr(player, "Item rewards: <items>", Placeholder.legacy("items", reward.getRewardText(), PRIMARY));
        sendTr(player, "XP reward: <experience:'0'>", number("experience", reward.getXpReward(), PRIMARY));
        if (defaults.enableEconomyPlugin) {
            double rewBonus = 1;
            Perk perk = perkLogic.getPerk(player);
            rewBonus += perk.getRewBonus();
            double currencyReward = reward.getCurrencyReward() * rewBonus;
            double percentage = rewBonus - 1.0;

            hookManager.getEconomyHook().ifPresent(hook -> {
                hook.depositPlayer(player, currencyReward);
                sendTr(player, "Currency reward: <primary><amount:'#,##0'><currency></primary> <secondary>(<bonus:'0%'>)</secondary>",
                    number("amount", currencyReward),
                    unparsed("currency", hook.getCurrenyName()),
                    number("bonus", percentage));
            });
        }

        if (reward.getPermissionReward() != null) {
            List<String> perms = Arrays.asList(reward.getPermissionReward().trim().split(" "));
            IslandInfo islandInfo = playerInfo.getIslandInfo();
            for (UUID memberUUID : islandInfo.getMemberUUIDs()) {
                if (memberUUID == null) {
                    continue;
                }
                PlayerInfo pi = plugin.getPlayerInfo(memberUUID);
                if (pi != null) {
                    pi.addPermissions(perms);
                }
            }
        }

        HashMap<Integer, ItemStack> leftOvers = player.getInventory().addItem(reward.getItemReward().toArray(new ItemStack[0]));
        for (ItemStack item : leftOvers.values()) {
            player.getWorld().dropItem(player.getLocation(), item);
        }
        if (!leftOvers.isEmpty()) {
            sendErrorTr(player, "Your inventory is full. <muted>Items were dropped on the ground.");
        }
        for (String cmd : reward.getCommands()) {
            String command = cmd.replaceAll("\\{challenge\\}", Matcher.quoteReplacement(challenge.getName()));
            command = command.replaceAll("\\{challengeName\\}", Matcher.quoteReplacement(challenge.getDisplayName()));
            plugin.execCommand(player, command, true);
        }
        if (!wasBroadcast) {
            IslandInfo island = playerInfo.getIslandInfo();
            if (island != null) {
                island.sendMessageToOnlineMembers(trLegacy("<player> has completed the <challenge> challenge!",
                    unparsed("player", playerInfo.getPlayerName(), PRIMARY),
                    Placeholder.legacy("challenge", challenge.getDisplayName(), PRIMARY)));
            }
        }
    }
}
