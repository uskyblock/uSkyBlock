package us.talabrek.ultimateskyblock.challenge;

import dk.lockfuglsang.minecraft.util.BlockRequirement;
import dk.lockfuglsang.minecraft.util.FormatUtil;
import dk.lockfuglsang.minecraft.util.ItemRequirement;
import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.message.Placeholder;
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
import static us.talabrek.ultimateskyblock.message.Msg.ERROR;
import static us.talabrek.ultimateskyblock.message.Msg.MUTED;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Msg.SECONDARY;
import static us.talabrek.ultimateskyblock.message.Placeholder.number;

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
                        lores.add(trLegacy("You can complete this <remaining> more time(s).",
                            MUTED,
                            number("remaining", getRepeatLimit() - timesCompleted, PRIMARY)));
                    }
                    if (cooldown.toDays() > 0) {
                        lores.add(trLegacy("Requirements will reset in <days> days.",
                            MUTED,
                            number("days", cooldown.toDays(), PRIMARY)));
                    } else if (cooldown.toHours() > 0) {
                        lores.add(trLegacy("Requirements will reset in <hours> hours.",
                            MUTED,
                            number("hours", cooldown.toHours(), PRIMARY)));
                    } else {
                        lores.add(trLegacy("Requirements will reset in <minutes> minutes.",
                            MUTED,
                            number("minutes", cooldown.toMinutes(), PRIMARY)));
                    }
                } else {
                    lores.add(trLegacy("This challenge is currently unavailable.", ERROR));
                    if (cooldown.toDays() > 0) {
                        lores.add(trLegacy("You can complete this again in <days> days.",
                            MUTED,
                            number("days", cooldown.toDays(), PRIMARY)));
                    } else if (cooldown.toHours() > 0) {
                        lores.add(trLegacy("You can complete this again in <hours> hours.",
                            MUTED,
                            number("hours", cooldown.toHours(), PRIMARY)));
                    } else {
                        lores.add(trLegacy("You can complete this again in <minutes> minutes.",
                            MUTED,
                            number("minutes", cooldown.toMinutes(), PRIMARY)));
                    }
                }
            }
            reward = getRepeatReward();
        }
        Map<ItemStack, Integer> requiredItemsForChallenge = getRequiredItems(timesCompleted);
        if (!requiredItemsForChallenge.isEmpty() || !requiredBlocks.isEmpty()
            || (requiredEntities != null && !requiredEntities.isEmpty())) {
            lores.add(trLegacy("This challenge requires:", MUTED));
        }
        List<String> details = new ArrayList<>();
        if (!requiredItemsForChallenge.isEmpty()) {
            for (Map.Entry<ItemStack, Integer> requiredItem : requiredItemsForChallenge.entrySet()) {
                if (wrappedDetails(details).size() >= MAX_DETAILS) {
                    details.add(trLegacy("and more...", MUTED));
                    break;
                }
                int requiredAmount = requiredItem.getValue();
                ItemStack requiredType = requiredItem.getKey();
                details.add(requiredAmount > 1
                    ? miniToLegacy("<count>x <muted><item>",
                    number("count", requiredAmount, SECONDARY),
                    legacyArg("item", ItemStackUtil.getItemName(requiredType)))
                    : miniToLegacy("<muted><item>", legacyArg("item", ItemStackUtil.getItemName(requiredType))));
            }
        }
        if (!requiredBlocks.isEmpty() && wrappedDetails(details).size() < MAX_DETAILS) {
            for (BlockRequirement blockRequirement : requiredBlocks) {
                if (wrappedDetails(details).size() >= MAX_DETAILS) {
                    details.add(trLegacy("and more...", MUTED));
                    break;
                }
                details.add(blockRequirement.amount() > 1
                    ? miniToLegacy("<count>x <muted><block>",
                    number("count", blockRequirement.amount(), SECONDARY),
                    legacyArg("block", ItemStackUtil.getBlockName(blockRequirement.type())))
                    : miniToLegacy("<muted><block>", legacyArg("block", ItemStackUtil.getBlockName(blockRequirement.type()))));
            }
        }
        if (requiredEntities != null && !requiredEntities.isEmpty() && wrappedDetails(details).size() < MAX_DETAILS) {
            for (EntityMatch entityMatch : requiredEntities) {
                if (wrappedDetails(details).size() >= MAX_DETAILS) {
                    details.add(trLegacy("and more...", MUTED));
                    break;
                }
                details.add(entityMatch.getCount() > 1
                    ? miniToLegacy("<count>x <muted><entity>",
                    number("count", entityMatch.getCount(), SECONDARY),
                    component("entity", entityMatch.getDisplayName()))
                    : miniToLegacy("<muted><entity>", component("entity", entityMatch.getDisplayName())));
            }
        }
        lores.addAll(wrappedDetails(details));
        if (type == Challenge.Type.PLAYER) {
            if (takeItems) {
                lores.add(trLegacy("Items will be traded for the reward.", MUTED));
            }
        } else if (type == Challenge.Type.ISLAND) {
            lores.add(trLegacy("Must be within <radius> meters.",
                MUTED,
                number("radius", getRadius(), PRIMARY)));
        }
        List<String> lines = wordWrap(reward.getRewardText(), 20, MAX_LINE);
        lores.add(trLegacy("Item reward: <reward-line>", MUTED, Placeholder.legacy("reward-line", lines.getFirst(), PRIMARY)));
        lores.addAll(lines.subList(1, lines.size()));
        if (withCurrency) {
            lores.add(trLegacy("Currency reward: <currency:'#,##0'>",
                MUTED,
                number("currency", reward.getCurrencyReward(), PRIMARY)));
        }
        lores.add(trLegacy("XP reward: <experience:'0'>",
            MUTED,
            number("experience", reward.getXpReward(), PRIMARY)));
        lores.add(trLegacy("Total completions: <times>",
            MUTED,
            number("times", completion.getTimesCompleted(), PRIMARY)));

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
            return wordWrap(trLegacy("Requires <requirement>", legacyArg("requirement", missingRequirement)), MAX_LINE);
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
