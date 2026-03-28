package us.talabrek.ultimateskyblock.challenge.catalog.bootstrap;

import dk.lockfuglsang.minecraft.util.BlockRequirement;
import dk.lockfuglsang.minecraft.util.ItemRequirement;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.challenge.Challenge;
import us.talabrek.ultimateskyblock.challenge.ChallengeDefaults;
import us.talabrek.ultimateskyblock.challenge.ChallengeKey;
import us.talabrek.ultimateskyblock.challenge.EntityMatch;
import us.talabrek.ultimateskyblock.challenge.Rank;
import us.talabrek.ultimateskyblock.challenge.Reward;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeCatalog;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRewards;
import us.talabrek.ultimateskyblock.challenge.catalog.RankDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.TextSpec;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.gameobject.ItemStackAmountProbabilitySpec;
import us.talabrek.ultimateskyblock.gameobject.ItemStackSpec;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ChallengeCatalogRuntimeAdapter {
    public @NotNull Map<String, Rank> adapt(
        @NotNull ChallengeCatalog catalog,
        @NotNull RuntimeConfig.Challenges challengeSettings
    ) {
        ChallengeDefaults defaults = new ChallengeDefaults(
            Duration.ofHours(144),
            false,
            "",
            "",
            "",
            0,
            challengeSettings.enableEconomyRewards(),
            challengeSettings.broadcast().enabled(),
            10,
            false,
            0
        );

        Map<String, Rank> ranks = new LinkedHashMap<>();
        Rank previous = null;
        for (RankDefinition rankDefinition : catalog.ranks()) {
            Rank rank = adaptRank(rankDefinition, previous, defaults);
            ranks.put(rank.getRankKey(), rank);
            previous = rank;
        }
        return ranks;
    }

    private @NotNull Rank adaptRank(@NotNull RankDefinition definition, Rank previous, ChallengeDefaults defaults) {
        Integer minimumCompletedChallenges = null;
        List<String> requiredChallenges = new ArrayList<>();
        List<String> requiredPermissions = new ArrayList<>();

        for (ChallengeRequirements.RankUnlockRequirement requirement : definition.unlockRequirements()) {
            if (requirement instanceof ChallengeRequirements.CompletedRankRequirement completedRankRequirement) {
                if (previous != null && previous.getRankKey().equals(completedRankRequirement.rankId().value())) {
                    minimumCompletedChallenges = completedRankRequirement.minimumCompletedChallenges();
                }
            } else if (requirement instanceof ChallengeRequirements.CompletedChallengesRequirement completedChallengesRequirement) {
                completedChallengesRequirement.challengeIds().stream().map(id -> id.value()).forEach(requiredChallenges::add);
            } else if (requirement instanceof ChallengeRequirements.PermissionRequirement permissionRequirement) {
                requiredPermissions.add(permissionRequirement.permission());
            }
        }

        Rank rank = new Rank(
            definition.id().value(),
            toLegacyText(definition.display().name()),
            definition.lockedDisplayItem(),
            Duration.ZERO,
            previous,
            defaults,
            minimumCompletedChallenges,
            requiredChallenges,
            requiredPermissions
        );

        for (ChallengeDefinition challengeDefinition : definition.challenges()) {
            rank.getChallenges().add(adaptChallenge(challengeDefinition, rank));
        }
        return rank;
    }

    private @NotNull Challenge adaptChallenge(@NotNull ChallengeDefinition definition, @NotNull Rank rank) {
        CompletionShape shape = CompletionShape.from(definition);
        List<String> requiredChallenges = new ArrayList<>();
        List<String> requiredPermissions = new ArrayList<>();
        double requiredLevel = 0d;
        for (ChallengeRequirements.ChallengeUnlockRequirement requirement : definition.unlockRequirements()) {
            if (requirement instanceof ChallengeRequirements.CompletedChallengesRequirement completedChallengesRequirement) {
                completedChallengesRequirement.challengeIds().stream().map(id -> id.value()).forEach(requiredChallenges::add);
            } else if (requirement instanceof ChallengeRequirements.PermissionRequirement permissionRequirement) {
                requiredPermissions.add(permissionRequirement.permission());
            } else if (requirement instanceof ChallengeRequirements.IslandLevelRequirement islandLevelRequirement) {
                requiredLevel = islandLevelRequirement.minimumLevel();
            }
        }

        return new Challenge(
            ChallengeKey.of(definition.id().value()),
            toLegacyText(definition.display().name()),
            toLegacyText(definition.display().description()),
            shape.type(),
            shape.requiredItems(),
            shape.requiredBlocks(),
            shape.requiredEntities(),
            requiredChallenges,
            requiredPermissions,
            shape.requiredLevel() > 0d ? shape.requiredLevel() : requiredLevel,
            rank,
            definition.repeatPolicy().resetWindow(),
            definition.display().displayItem(),
            null,
            definition.lockedDisplayItem(),
            0,
            definition.properties().consumeItemsOnCompletion(),
            shape.radius(),
            adaptReward(definition.firstCompletionReward()),
            definition.repeatPolicy().repeatable() ? adaptReward(definition.repeatReward().isEmpty() ? definition.firstCompletionReward() : definition.repeatReward()) : null,
            definition.repeatPolicy().repeatLimit()
        );
    }

    private @NotNull Reward adaptReward(@NotNull us.talabrek.ultimateskyblock.challenge.catalog.RewardBundle bundle) {
        List<ItemStackAmountProbabilitySpec> itemRewards = new ArrayList<>();
        List<String> permissions = new ArrayList<>();
        int currency = 0;
        int xp = 0;
        List<String> commands = new ArrayList<>();
        List<String> summary = new ArrayList<>();

        for (ChallengeRewards.RewardAction action : bundle.actions()) {
            if (action instanceof ChallengeRewards.ItemReward itemReward) {
                itemRewards.addAll(itemReward.itemSpecs());
                for (ItemStackAmountProbabilitySpec spec : itemReward.itemSpecs()) {
                    summary.add(spec.item().amount() + "x " + itemName(spec.item().prototype()));
                }
            } else if (action instanceof ChallengeRewards.EconomyReward economyReward) {
                currency += economyReward.amount();
                summary.add(economyReward.amount() + " currency");
            } else if (action instanceof ChallengeRewards.ExperienceReward experienceReward) {
                xp += experienceReward.amount();
                summary.add(experienceReward.amount() + " xp");
            } else if (action instanceof ChallengeRewards.PermissionReward permissionReward) {
                permissions.addAll(permissionReward.permissions());
                summary.addAll(permissionReward.permissions());
            } else if (action instanceof ChallengeRewards.CommandReward commandReward) {
                for (ChallengeRewards.CommandSpec command : commandReward.commands()) {
                    commands.add(serializeCommand(command));
                    summary.add(command.command());
                }
            }
        }

        return new Reward(
            String.join(", ", summary),
            itemRewards,
            permissions.isEmpty() ? null : permissions.getFirst(),
            currency,
            xp,
            commands
        );
    }

    private static @NotNull String serializeCommand(@NotNull ChallengeRewards.CommandSpec command) {
        return switch (command.execution()) {
            case PLAYER -> command.command();
            case OP -> "op:" + command.command();
            case CONSOLE -> "console:" + command.command();
        };
    }

    private static @NotNull String itemName(@NotNull ItemStackSpec itemStackSpec) {
        return itemStackSpec.create().getType().getKey().toString().toLowerCase(Locale.ROOT);
    }

    private static @NotNull String toLegacyText(@NotNull TextSpec spec) {
        if (spec.source().isBlank()) {
            return "";
        }
        return LegacyComponentSerializer.legacySection().serialize(MiniMessage.miniMessage().deserialize(spec.source()));
    }

    private record CompletionShape(
        Challenge.Type type,
        List<ItemRequirement> requiredItems,
        List<BlockRequirement> requiredBlocks,
        List<EntityMatch> requiredEntities,
        double requiredLevel,
        int radius
    ) {
        static @NotNull CompletionShape from(@NotNull ChallengeDefinition definition) {
            List<ItemRequirement> requiredItems = new ArrayList<>();
            List<BlockRequirement> requiredBlocks = new ArrayList<>();
            List<EntityMatch> requiredEntities = new ArrayList<>();
            double requiredLevel = 0d;
            int radius = 10;
            EnumSet<Challenge.Type> types = EnumSet.noneOf(Challenge.Type.class);

            for (ChallengeRequirements.CompletionRequirement requirement : definition.completionRequirements()) {
                if (requirement instanceof ChallengeRequirements.InventoryItemsRequirement inventoryItemsRequirement) {
                    types.add(Challenge.Type.PLAYER);
                    for (ChallengeRequirements.ItemRequirementSpec spec : inventoryItemsRequirement.items()) {
                        requiredItems.add(new ItemRequirement(
                            spec.item().create(),
                            spec.amount(),
                            spec.progression().operator(),
                            spec.progression().increment()
                        ));
                    }
                } else if (requirement instanceof ChallengeRequirements.IslandBlocksRequirement islandBlocksRequirement) {
                    types.add(Challenge.Type.ISLAND);
                    radius = islandBlocksRequirement.radius();
                    for (ChallengeRequirements.BlockRequirementSpec spec : islandBlocksRequirement.blocks()) {
                        requiredBlocks.add(new BlockRequirement(spec.prototype(), spec.amount()));
                    }
                } else if (requirement instanceof ChallengeRequirements.EntityPresenceRequirement entityPresenceRequirement) {
                    types.add(Challenge.Type.ISLAND);
                    radius = entityPresenceRequirement.radius();
                    for (ChallengeRequirements.EntityRequirementSpec spec : entityPresenceRequirement.entities()) {
                        requiredEntities.add(new EntityMatch(spec.type(), spec.metadata(), spec.count()));
                    }
                } else if (requirement instanceof ChallengeRequirements.IslandLevelRequirement islandLevelRequirement) {
                    types.add(Challenge.Type.ISLAND_LEVEL);
                    requiredLevel = islandLevelRequirement.minimumLevel();
                }
            }

            if (types.isEmpty()) {
                return new CompletionShape(Challenge.Type.PLAYER, requiredItems, requiredBlocks, requiredEntities, requiredLevel, radius);
            }
            if (types.size() > 1) {
                throw new IllegalArgumentException("Challenge '" + definition.id().value()
                    + "' mixes completion requirement types that are not supported by the legacy runtime adapter.");
            }
            return new CompletionShape(types.iterator().next(), requiredItems, requiredBlocks, requiredEntities, requiredLevel, radius);
        }
    }
}
