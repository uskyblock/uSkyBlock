package us.talabrek.ultimateskyblock.challenge.catalog;

import us.talabrek.ultimateskyblock.gameobject.ItemStackAmountProbabilitySpec;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class ChallengeRewards {
    private ChallengeRewards() {
    }

    public sealed interface RewardAction permits ItemReward, EconomyReward, ExperienceReward, PermissionReward, BiomeReward, CommandReward {
    }

    public record ItemReward(List<ItemStackAmountProbabilitySpec> itemSpecs) implements RewardAction {
        public ItemReward {
            itemSpecs = List.copyOf(Objects.requireNonNull(itemSpecs, "itemSpecs"));
        }
    }

    public record EconomyReward(int amount) implements RewardAction {
    }

    public record ExperienceReward(int amount) implements RewardAction {
    }

    public record PermissionReward(List<String> permissions) implements RewardAction {
        public PermissionReward {
            permissions = List.copyOf(Objects.requireNonNull(permissions, "permissions"));
        }
    }

    /**
     * Unlocks biomes for the whole island. The unlock is derived from completed challenges, not
     * materialized: the island has a biome as long as a completed challenge rewards it.
     */
    public record BiomeReward(List<String> biomes) implements RewardAction {
        public BiomeReward {
            biomes = List.copyOf(Objects.requireNonNull(biomes, "biomes")).stream()
                .map(biome -> biome.toLowerCase(Locale.ROOT))
                .toList();
        }
    }

    public record CommandReward(List<CommandSpec> commands) implements RewardAction {
        public CommandReward {
            commands = List.copyOf(Objects.requireNonNull(commands, "commands"));
        }
    }

    public record CommandSpec(CommandExecution execution, String command) {
        public CommandSpec {
            execution = Objects.requireNonNull(execution, "execution");
            command = Objects.requireNonNull(command, "command").trim();
            if (command.isEmpty()) {
                throw new IllegalArgumentException("command cannot be blank");
            }
        }
    }

    public enum CommandExecution {
        PLAYER,
        OP,
        CONSOLE
    }
}
