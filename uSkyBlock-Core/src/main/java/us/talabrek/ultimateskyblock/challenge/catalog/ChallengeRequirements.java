package us.talabrek.ultimateskyblock.challenge.catalog;

import dk.lockfuglsang.minecraft.util.ItemRequirement;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import us.talabrek.ultimateskyblock.gameobject.ItemStackSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class ChallengeRequirements {
    private ChallengeRequirements() {
    }

    public sealed interface RankUnlockRequirement permits CompletedChallengesRequirement, CompletedRankRequirement, PermissionRequirement, IslandLevelRequirement {
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
        implements RankUnlockRequirement, ChallengeUnlockRequirement, CompletionRequirement {
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

    /**
     * What an inventory hand-in item accepts: one exact item, any item carrying a data pack tag
     * ({@code #minecraft:beds}), or any-of an explicit list of matchers.
     */
    public sealed interface ItemMatcher permits ExactItem, ItemTag, AnyOfItems {
        boolean matches(ItemStack candidate);
    }

    public record ExactItem(ItemStackSpec item) implements ItemMatcher {
        public ExactItem {
            item = Objects.requireNonNull(item, "item");
        }

        @Override
        public boolean matches(ItemStack candidate) {
            return candidate.isSimilar(item.create());
        }
    }

    public record ItemTag(Tag<Material> tag, String key) implements ItemMatcher {
        public ItemTag {
            tag = Objects.requireNonNull(tag, "tag");
            key = Objects.requireNonNull(key, "key").trim().toLowerCase(Locale.ROOT);
            if (key.isEmpty()) {
                throw new IllegalArgumentException("key cannot be blank");
            }
        }

        @Override
        public boolean matches(ItemStack candidate) {
            return tag.isTagged(candidate.getType());
        }
    }

    public record AnyOfItems(List<ItemMatcher> matchers) implements ItemMatcher {
        public AnyOfItems {
            matchers = List.copyOf(Objects.requireNonNull(matchers, "matchers"));
            if (matchers.isEmpty()) {
                throw new IllegalArgumentException("matchers cannot be empty");
            }
        }

        @Override
        public boolean matches(ItemStack candidate) {
            return matchers.stream().anyMatch(matcher -> matcher.matches(candidate));
        }
    }

    public record ItemRequirementSpec(ItemMatcher matcher, int amount, ItemAmountProgression progression) {
        public ItemRequirementSpec {
            matcher = Objects.requireNonNull(matcher, "matcher");
            progression = Objects.requireNonNull(progression, "progression");
            if (amount < 0) {
                throw new IllegalArgumentException("amount cannot be negative");
            }
        }

        public ItemRequirementSpec(ItemStackSpec item, int amount, ItemAmountProgression progression) {
            this(new ExactItem(item), amount, progression);
        }

        public boolean matches(ItemStack candidate) {
            return matcher.matches(candidate);
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

    /**
     * What an on-island block requirement accepts: one exact block material, any block carrying a
     * data pack tag ({@code #minecraft:wooden_doors}), or any-of an explicit list of matchers.
     * Island block scans only ever compare materials, so a matcher tests a {@link Material}.
     */
    public sealed interface BlockMatcher permits ExactBlock, BlockTag, AnyOfBlocks {
        boolean matches(Material candidate);
    }

    public record ExactBlock(Material material) implements BlockMatcher {
        public ExactBlock {
            material = Objects.requireNonNull(material, "material");
        }

        @Override
        public boolean matches(Material candidate) {
            return candidate == material;
        }
    }

    public record BlockTag(Tag<Material> tag, String key) implements BlockMatcher {
        public BlockTag {
            tag = Objects.requireNonNull(tag, "tag");
            key = Objects.requireNonNull(key, "key").trim().toLowerCase(Locale.ROOT);
            if (key.isEmpty()) {
                throw new IllegalArgumentException("key cannot be blank");
            }
        }

        @Override
        public boolean matches(Material candidate) {
            return tag.isTagged(candidate);
        }
    }

    public record AnyOfBlocks(List<BlockMatcher> matchers) implements BlockMatcher {
        public AnyOfBlocks {
            matchers = List.copyOf(Objects.requireNonNull(matchers, "matchers"));
            if (matchers.isEmpty()) {
                throw new IllegalArgumentException("matchers cannot be empty");
            }
        }

        @Override
        public boolean matches(Material candidate) {
            return matchers.stream().anyMatch(matcher -> matcher.matches(candidate));
        }
    }

    public record BlockRequirementSpec(BlockMatcher matcher, int amount) {
        public BlockRequirementSpec {
            matcher = Objects.requireNonNull(matcher, "matcher");
            if (amount < 0) {
                throw new IllegalArgumentException("amount cannot be negative");
            }
        }

        public BlockRequirementSpec(BlockData prototype, int amount) {
            this(new ExactBlock(Objects.requireNonNull(prototype, "prototype").getMaterial()), amount);
        }

        public boolean matches(Material candidate) {
            return matcher.matches(candidate);
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
