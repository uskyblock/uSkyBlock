package us.talabrek.ultimateskyblock.challenge;

import dk.lockfuglsang.minecraft.util.BlockRequirement;
import dk.lockfuglsang.minecraft.util.FormatUtil;
import dk.lockfuglsang.minecraft.util.ItemRequirement;
import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static dk.lockfuglsang.minecraft.po.I18nUtil.legacyArg;
import static dk.lockfuglsang.minecraft.po.I18nUtil.miniToLegacy;
import static dk.lockfuglsang.minecraft.po.I18nUtil.trLegacy;
import static dk.lockfuglsang.minecraft.util.FormatUtil.prefix;
import static dk.lockfuglsang.minecraft.util.FormatUtil.wordWrap;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * The data-object for a challenge
 */
public class Challenge {
    public static final int MAX_DETAILS = 11;
    public static final int MAX_LINE = 30;

    public enum Type {
        PLAYER, ISLAND, ISLAND_LEVEL;

        static Type from(String s) {
            if (s == null || s.trim().isEmpty() || s.trim().equalsIgnoreCase("onplayer")) {
                return PLAYER;
            } else if (s.equalsIgnoreCase("islandlevel")) {
                return ISLAND_LEVEL;
            }
            return ISLAND;
        }
    }

    private final ChallengeKey id;
    private final String description;
    private final String displayName;
    private final Type type;
    private final List<ItemRequirement> requiredItems;
    private final List<BlockRequirement> requiredBlocks;
    private final List<EntityMatch> requiredEntities;
    private final List<String> requiredChallenges;
    private final double requiredLevel;
    private final Rank rank;
    private final Duration resetDuration;
    private final ItemStack displayItem;
    private final String tool;
    private final ItemStack lockedItem;
    private final int offset;
    private final boolean takeItems;
    private final int radius;
    private final Reward reward;
    private final Reward repeatReward;
    private final int repeatLimit;

    public Challenge(ChallengeKey id, String displayName, String description, Type type, List<ItemRequirement> requiredItems,
                     @NotNull List<BlockRequirement> requiredBlocks, List<EntityMatch> requiredEntities,
                     List<String> requiredChallenges, double requiredLevel, Rank rank,
                     Duration resetDuration, ItemStack displayItem, String tool, ItemStack lockedItem, int offset,
                     boolean takeItems, int radius, Reward reward, Reward repeatReward, int repeatLimit) {
        this.id = id;
        this.displayName = displayName;
        this.type = type;
        this.requiredItems = requiredItems;
        this.requiredBlocks = requiredBlocks;
        this.requiredEntities = requiredEntities;
        this.requiredChallenges = requiredChallenges;
        this.requiredLevel = requiredLevel;
        this.rank = rank;
        this.resetDuration = resetDuration;
        this.displayItem = displayItem;
        this.tool = tool;
        this.lockedItem = lockedItem;
        this.offset = offset;
        this.takeItems = takeItems;
        this.radius = radius;
        this.reward = reward;
        this.repeatReward = repeatReward;
        this.description = description;
        this.repeatLimit = repeatLimit;
    }

    public boolean isRepeatable() {
        return repeatReward != null;
    }

    public ChallengeKey getId() {
        return id;
    }

    /**
     * @deprecated Use {@link #getId()} instead.
     */
    @Deprecated
    public String getName() {
        return getId().id();
    }

    public String getDisplayName() {
        return FormatUtil.normalize(displayName);
    }

    public Type getType() {
        return type;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public int getRadius() {
        return radius;
    }

    public double getRequiredLevel() {
        return requiredLevel;
    }

    @NotNull
    public Map<ItemStack, Integer> getRequiredItems(int timesCompleted) {
        return this.requiredItems.stream().collect(Collectors.toUnmodifiableMap(
            item -> item.type().clone(),
            item -> item.amountForRepetitions(timesCompleted)
        ));
    }

    @NotNull
    public List<BlockRequirement> getRequiredBlocks() {
        return requiredBlocks;
    }

    public List<EntityMatch> getRequiredEntities() {
        return requiredEntities;
    }

    public List<String> getRequiredChallenges() {
        return requiredChallenges;
    }

    public Rank getRank() {
        return rank;
    }

    public Duration getResetDuration() {
        return resetDuration;
    }

    public ItemStack getDisplayItem(ChallengeCompletion completion, boolean withCurrency) {
        int timesCompleted = completion.getTimesCompletedInCooldown();
        ItemStack currentChallengeItem = getDisplayItem();
        ItemMeta meta = currentChallengeItem.getItemMeta();
        List<String> lores = new ArrayList<>(prefix(wordWrap(getDescription(), MAX_LINE), "\u00a77"));
        Reward reward = getReward();
        if (completion.getTimesCompleted() > 0 && isRepeatable()) {
            currentChallengeItem.setAmount(completion.getTimesCompleted() < currentChallengeItem.getMaxStackSize() ? completion.getTimesCompleted() : currentChallengeItem.getMaxStackSize());
            if (completion.isOnCooldown()) {
                Duration cooldown = completion.getCooldown();
                if (timesCompleted < getRepeatLimit() || getRepeatLimit() <= 0) {
                    if (getRepeatLimit() > 0) {
                        lores.add(trLegacy("<muted>You can complete this <primary><remaining></primary> more time(s).",
                            unparsed("remaining", String.valueOf(getRepeatLimit() - timesCompleted))));
                    }
                    if (cooldown.toDays() > 0) {
                        lores.add(trLegacy("<muted>Requirements will reset in <primary><days></primary> days.",
                            unparsed("days", String.valueOf(cooldown.toDays()))));
                    } else if (cooldown.toHours() > 0) {
                        lores.add(trLegacy("<muted>Requirements will reset in <primary><hours></primary> hours.",
                            unparsed("hours", String.valueOf(cooldown.toHours()))));
                    } else {
                        lores.add(trLegacy("<muted>Requirements will reset in <primary><minutes></primary> minutes.",
                            unparsed("minutes", String.valueOf(cooldown.toMinutes()))));
                    }
                } else {
                    lores.add(trLegacy("<error>This challenge is currently unavailable."));
                    if (cooldown.toDays() > 0) {
                        lores.add(trLegacy("<muted>You can complete this again in <primary><days></primary> days.",
                            unparsed("days", String.valueOf(cooldown.toDays()))));
                    } else if (cooldown.toHours() > 0) {
                        lores.add(trLegacy("<muted>You can complete this again in <primary><hours></primary> hours.",
                            unparsed("hours", String.valueOf(cooldown.toHours()))));
                    } else {
                        lores.add(trLegacy("<muted>You can complete this again in <primary><minutes></primary> minutes.",
                            unparsed("minutes", String.valueOf(cooldown.toMinutes()))));
                    }
                }
            }
            reward = getRepeatReward();
        }
        Map<ItemStack, Integer> requiredItemsForChallenge = getRequiredItems(timesCompleted);
        if (!requiredItemsForChallenge.isEmpty() || !requiredBlocks.isEmpty()
            || (requiredEntities != null && !requiredEntities.isEmpty())) {
            lores.add(trLegacy("<muted>This challenge requires:"));
        }
        List<String> details = new ArrayList<>();
        if (!requiredItemsForChallenge.isEmpty()) {
            for (Map.Entry<ItemStack, Integer> requiredItem : requiredItemsForChallenge.entrySet()) {
                if (wrappedDetails(details).size() >= MAX_DETAILS) {
                    details.add(trLegacy("<muted>and more..."));
                    break;
                }
                int requiredAmount = requiredItem.getValue();
                ItemStack requiredType = requiredItem.getKey();
                details.add(requiredAmount > 1
                    ? miniToLegacy("<secondary><count></secondary>x <muted><item>",
                        unparsed("count", String.valueOf(requiredAmount)),
                        legacyArg("item", ItemStackUtil.getItemName(requiredType)))
                    : miniToLegacy("<muted><item>", legacyArg("item", ItemStackUtil.getItemName(requiredType))));
            }
        }
        if (!requiredBlocks.isEmpty() && wrappedDetails(details).size() < MAX_DETAILS) {
            for (BlockRequirement blockRequirement : requiredBlocks) {
                if (wrappedDetails(details).size() >= MAX_DETAILS) {
                    details.add(trLegacy("<muted>and more..."));
                    break;
                }
                details.add(blockRequirement.amount() > 1
                    ? miniToLegacy("<secondary><count></secondary>x <muted><block>",
                        unparsed("count", String.valueOf(blockRequirement.amount())),
                        legacyArg("block", ItemStackUtil.getBlockName(blockRequirement.type())))
                    : miniToLegacy("<muted><block>", legacyArg("block", ItemStackUtil.getBlockName(blockRequirement.type()))));
            }
        }
        if (requiredEntities != null && !requiredEntities.isEmpty() && wrappedDetails(details).size() < MAX_DETAILS) {
            for (EntityMatch entityMatch : requiredEntities) {
                if (wrappedDetails(details).size() >= MAX_DETAILS) {
                    details.add(trLegacy("<muted>and more..."));
                    break;
                }
                details.add(entityMatch.getCount() > 1
                    ? miniToLegacy("<secondary><count></secondary>x <muted><entity>",
                        unparsed("count", String.valueOf(entityMatch.getCount())),
                        component("entity", entityMatch.getDisplayName()))
                    : miniToLegacy("<muted><entity>", component("entity", entityMatch.getDisplayName())));
            }
        }
        lores.addAll(wrappedDetails(details));
        if (type == Challenge.Type.PLAYER) {
            if (takeItems) {
                lores.add(trLegacy("<muted>Items will be traded for the reward."));
            }
        } else if (type == Challenge.Type.ISLAND) {
            lores.add(trLegacy("<muted>Must be within <primary><radius></primary> meters.",
                unparsed("radius", String.valueOf(getRadius()))));
        }
        List<String> lines = wordWrap(reward.getRewardText(), 20, MAX_LINE);
        lores.add(trLegacy("<muted>Item reward: <primary><reward-line></primary>", legacyArg("reward-line", lines.getFirst())));
        lores.addAll(lines.subList(1, lines.size()));
        if (withCurrency) {
            lores.add(trLegacy("<muted>Currency reward: <primary><currency></primary>",
                unparsed("currency", String.valueOf(reward.getCurrencyReward()))));
        }
        lores.add(trLegacy("<muted>XP reward: <primary><experience></primary>",
            unparsed("experience", String.valueOf(reward.getXpReward()))));
        lores.add(trLegacy("<muted>Total completions: <primary><times></primary>",
            unparsed("times", String.valueOf(completion.getTimesCompleted()))));

        meta.setLore(lores);
        currentChallengeItem.setItemMeta(meta);
        return currentChallengeItem;
    }

    public int getOffset() {
        return offset;
    }

    private List<String> wrappedDetails(List<String> details) {
        return wordWrap(String.join(", ", details), MAX_LINE);
    }

    public ItemStack getDisplayItem() {
        return ItemStackUtil.asDisplayItem(displayItem); // Copy
    }

    public String getTool() {
        return tool;
    }

    public ItemStack getLockedDisplayItem() {
        return lockedItem != null ? new ItemStack(lockedItem) : null;
    }

    public boolean isTakeItems() {
        return takeItems;
    }

    public Reward getReward() {
        return reward;
    }

    public Reward getRepeatReward() {
        return repeatReward;
    }

    public int getRepeatLimit() {
        return repeatLimit;
    }

    public List<String> getMissingRequirements(PlayerInfo playerInfo) {
        String missingRequirement = ChallengeFormat.getMissingRequirement(playerInfo, requiredChallenges, uSkyBlock.getInstance().getChallengeLogic());
        if (missingRequirement != null) {
            return wordWrap(trLegacy("<muted>Requires <requirement>", legacyArg("requirement", missingRequirement)), MAX_LINE);
        }
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return "Challenge{" +
            "id='" + id + '\'' +
            ", type=" + type +
            ", requiredItems='" + requiredItems + '\'' +
            ", rank='" + rank + '\'' +
            ", resetDuration=" + resetDuration +
            ", displayItem=" + displayItem +
            ", takeItems=" + takeItems +
            ", reward=" + reward +
            ", repeatReward=" + repeatReward +
            ", repeatLimit=" + repeatLimit +
            '}';
    }
}
