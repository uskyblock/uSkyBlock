package us.talabrek.ultimateskyblock.challenge.view;

import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import dk.lockfuglsang.minecraft.util.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.challenge.ChallengeCompletion;
import us.talabrek.ultimateskyblock.challenge.ChallengeText;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.BlockRequirementSpec;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.CompletionRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.EntityPresenceRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.EntityRequirementSpec;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.InventoryItemsRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.IslandBlocksRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.IslandLevelRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.ItemRequirementSpec;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRewards.BiomeReward;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRewards.CommandReward;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRewards.EconomyReward;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRewards.ExperienceReward;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRewards.ItemReward;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRewards.PermissionReward;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRewards.RewardAction;
import us.talabrek.ultimateskyblock.challenge.catalog.RewardBundle;
import us.talabrek.ultimateskyblock.gameobject.ItemStackAmountProbabilitySpec;
import us.talabrek.ultimateskyblock.util.ComponentLineSplitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static us.talabrek.ultimateskyblock.message.Msg.MUTED;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Msg.SECONDARY;
import static us.talabrek.ultimateskyblock.message.Placeholder.component;
import static us.talabrek.ultimateskyblock.message.Placeholder.number;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;

/**
 * Player-facing lines describing a challenge: description, requirements, rewards, and progress.
 * Shared between the challenge menu and the info command.
 */
public final class ChallengeLore {
    private ChallengeLore() {
    }

    public static @NotNull List<Component> describe(
        @NotNull ChallengeDefinition challenge,
        @Nullable ChallengeCompletion completion,
        boolean economyEnabled
    ) {
        List<Component> lines = new ArrayList<>();
        if (!challenge.display().description().source().isBlank()) {
            lines.addAll(ComponentLineSplitter.splitLines(ChallengeText.render(challenge.display().description()).applyFallbackStyle(MUTED)));
        }
        lines.addAll(requirementLines(challenge, completion));
        lines.addAll(rewardLines(challenge, completion, economyEnabled));
        lines.addAll(statusLines(challenge, completion));
        return lines;
    }

    private static List<Component> requirementLines(@NotNull ChallengeDefinition challenge, @Nullable ChallengeCompletion completion) {
        List<Component> lines = new ArrayList<>();
        int repetitions = completion != null ? completion.getTimesCompletedInCooldown() : 0;
        for (CompletionRequirement requirement : challenge.completionRequirements()) {
            switch (requirement) {
                case InventoryItemsRequirement items -> {
                    lines.add(tr("Required items:", MUTED));
                    for (ItemRequirementSpec spec : items.items()) {
                        lines.add(tr(" - <amount> x <item>", MUTED,
                            number("amount", spec.amountForRepetitions(repetitions), PRIMARY),
                            component("item", ChallengeText.itemName(spec), PRIMARY)));
                    }
                }
                case IslandBlocksRequirement blocks -> {
                    lines.add(tr("Required on your island (within <radius> blocks):", MUTED, number("radius", blocks.radius(), PRIMARY)));
                    for (BlockRequirementSpec spec : blocks.blocks()) {
                        lines.add(tr(" - <amount> x <item>", MUTED,
                            number("amount", spec.amount(), PRIMARY),
                            component("item", ChallengeText.blockName(spec.matcher()), PRIMARY)));
                    }
                }
                case EntityPresenceRequirement entities -> {
                    lines.add(tr("Required nearby (within <radius> blocks):", MUTED, number("radius", entities.radius(), PRIMARY)));
                    for (EntityRequirementSpec spec : entities.entities()) {
                        lines.add(tr(" - <amount> x <item>", MUTED,
                            number("amount", spec.count(), PRIMARY),
                            unparsed("item", humanize(spec.type().getKey().getKey()), PRIMARY)));
                    }
                }
                case IslandLevelRequirement level -> lines.add(tr("Requires island level <level>", MUTED,
                    number("level", level.minimumLevel(), PRIMARY)));
            }
        }
        return lines;
    }

    private static List<Component> rewardLines(
        @NotNull ChallengeDefinition challenge,
        @Nullable ChallengeCompletion completion,
        boolean economyEnabled
    ) {
        boolean firstCompletion = completion == null || completion.getTimesCompleted() == 0;
        RewardBundle bundle = firstCompletion || challenge.repeatReward().isEmpty()
            ? challenge.firstCompletionReward()
            : challenge.repeatReward();
        List<Component> lines = new ArrayList<>();
        for (RewardAction action : bundle.actions()) {
            switch (action) {
                case ItemReward itemReward -> {
                    for (ItemStackAmountProbabilitySpec spec : itemReward.itemSpecs()) {
                        ItemStack item = spec.item().prototype().create();
                        Component line = tr("Reward: <amount> x <item>", MUTED,
                            number("amount", spec.item().amount(), SECONDARY),
                            component("item", ItemStackUtil.getItemName(item), SECONDARY));
                        if (spec.probability() < 1.0d) {
                            line = line.append(tr(" (<chance:'0%'> chance)", MUTED, number("chance", spec.probability())));
                        }
                        lines.add(line);
                    }
                }
                case ExperienceReward xp -> lines.add(tr("XP reward: <experience:'0'>", MUTED, number("experience", xp.amount(), SECONDARY)));
                case EconomyReward economy -> {
                    if (economyEnabled) {
                        lines.add(tr("Currency reward: <amount:'#,##0'>", MUTED, number("amount", economy.amount(), SECONDARY)));
                    }
                }
                case BiomeReward biomes -> lines.add(tr("Unlocks island biome: <biomes>", MUTED,
                    unparsed("biomes", String.join(", ", biomes.biomes()), SECONDARY)));
                case PermissionReward ignored -> {
                    // Permission internals are not player-facing.
                }
                case CommandReward ignored -> {
                    // Command internals are not player-facing.
                }
            }
        }
        return lines;
    }

    private static List<Component> statusLines(@NotNull ChallengeDefinition challenge, @Nullable ChallengeCompletion completion) {
        List<Component> lines = new ArrayList<>();
        if (completion == null || completion.getTimesCompleted() == 0) {
            return lines;
        }
        lines.add(tr("Completed <count> times", SECONDARY, number("count", completion.getTimesCompleted())));
        if (!challenge.repeatPolicy().repeatable()) {
            lines.add(tr("This challenge is not repeatable.", MUTED));
        } else if (completion.isOnCooldown()) {
            if (!challenge.repeatPolicy().isUnlimited()
                && completion.getTimesCompletedInCooldown() >= challenge.repeatPolicy().repeatLimit()) {
                lines.add(tr("Repeatable again in <time>", MUTED,
                    unparsed("time", TimeUtil.durationAsString(completion.getCooldown()), PRIMARY)));
            }
        }
        return lines;
    }

    private static String humanize(String key) {
        return key.replace('_', ' ').toLowerCase(Locale.ROOT);
    }
}
