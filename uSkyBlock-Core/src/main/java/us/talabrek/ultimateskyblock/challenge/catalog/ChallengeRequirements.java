package us.talabrek.ultimateskyblock.challenge.catalog;

import dk.lockfuglsang.minecraft.util.ItemRequirement;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import us.talabrek.ultimateskyblock.gameobject.ItemStackSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ChallengeRequirements {
    private ChallengeRequirements() {
    }

    public sealed interface RankUnlockRequirement permits CompletedChallengesRequirement, CompletedRankRequirement, PermissionRequirement {
    }

    public sealed interface ChallengeUnlockRequirement permits CompletedChallengesRequirement, CompletedRankRequirement, PermissionRequirement, IslandLevelRequirement {
    }

    public sealed interface CompletionRequirement permits InventoryItemsRequirement, IslandBlocksRequirement, EntityPresenceRequirement, IslandLevelRequirement {
    }

    public record CompletedChallengesRequirement(List<ChallengeId> challengeIds)
        implements RankUnlockRequirement, ChallengeUnlockRequirement {
        public CompletedChallengesRequirement {
            challengeIds = List.copyOf(Objects.requireNonNull(challengeIds, "challengeIds"));
        }
    }

    public record CompletedRankRequirement(RankId rankId, int minimumCompletedChallenges)
        implements RankUnlockRequirement, ChallengeUnlockRequirement {
        public CompletedRankRequirement {
            rankId = Objects.requireNonNull(rankId, "rankId");
            if (minimumCompletedChallenges < 0) {
                throw new IllegalArgumentException("minimumCompletedChallenges cannot be negative");
            }
        }
    }

    public record PermissionRequirement(String permission)
        implements RankUnlockRequirement, ChallengeUnlockRequirement {
        public PermissionRequirement {
            permission = Objects.requireNonNull(permission, "permission").trim();
            if (permission.isEmpty()) {
                throw new IllegalArgumentException("permission cannot be blank");
            }
        }
    }

    public record IslandLevelRequirement(double minimumLevel)
        implements ChallengeUnlockRequirement, CompletionRequirement {
        public IslandLevelRequirement {
            if (minimumLevel < 0) {
                throw new IllegalArgumentException("minimumLevel cannot be negative");
            }
        }
    }

    public record InventoryItemsRequirement(List<ItemRequirementSpec> items) implements CompletionRequirement {
        public InventoryItemsRequirement {
            items = List.copyOf(Objects.requireNonNull(items, "items"));
        }
    }

    public record IslandBlocksRequirement(List<BlockRequirementSpec> blocks, int radius) implements CompletionRequirement {
        public IslandBlocksRequirement {
            blocks = List.copyOf(Objects.requireNonNull(blocks, "blocks"));
            if (radius < 0) {
                throw new IllegalArgumentException("radius cannot be negative");
            }
        }
    }

    public record EntityPresenceRequirement(List<EntityRequirementSpec> entities, int radius) implements CompletionRequirement {
        public EntityPresenceRequirement {
            entities = List.copyOf(Objects.requireNonNull(entities, "entities"));
            if (radius < 0) {
                throw new IllegalArgumentException("radius cannot be negative");
            }
        }
    }

    public record ItemRequirementSpec(ItemStackSpec item, int amount, ItemAmountProgression progression) {
        public ItemRequirementSpec {
            item = Objects.requireNonNull(item, "item");
            progression = Objects.requireNonNull(progression, "progression");
            if (amount < 0) {
                throw new IllegalArgumentException("amount cannot be negative");
            }
        }

        public int amountForRepetitions(int repetitions) {
            return (int) Math.floor(progression.operator().apply(amount, progression.increment(), repetitions));
        }
    }

    public record ItemAmountProgression(ItemRequirement.Operator operator, double increment) {
        public ItemAmountProgression {
            operator = Objects.requireNonNull(operator, "operator");
        }

        public static ItemAmountProgression none() {
            return new ItemAmountProgression(ItemRequirement.Operator.NONE, ItemRequirement.Operator.NONE.getNeutralElement());
        }
    }

    public static final class BlockRequirementSpec {
        private final BlockData prototype;
        private final int amount;

        public BlockRequirementSpec(BlockData prototype, int amount) {
            this.prototype = Objects.requireNonNull(prototype, "prototype").clone();
            if (amount < 0) {
                throw new IllegalArgumentException("amount cannot be negative");
            }
            this.amount = amount;
        }

        public BlockData prototype() {
            return prototype.clone();
        }

        public int amount() {
            return amount;
        }
    }

    public record EntityRequirementSpec(EntityType type, Map<String, Object> metadata, int count) {
        public EntityRequirementSpec {
            type = Objects.requireNonNull(type, "type");
            metadata = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNull(metadata, "metadata")));
            if (count < 0) {
                throw new IllegalArgumentException("count cannot be negative");
            }
        }
    }
}
