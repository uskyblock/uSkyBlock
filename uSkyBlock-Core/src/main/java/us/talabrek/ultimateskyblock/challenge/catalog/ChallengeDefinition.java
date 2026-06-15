package us.talabrek.ultimateskyblock.challenge.catalog;

import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.ChallengeUnlockRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.CompletionRequirement;
import us.talabrek.ultimateskyblock.gameobject.ItemStackSpec;

import java.util.List;
import java.util.Objects;

public record ChallengeDefinition(
    ChallengeId id,
    DisplaySpec display,
    ItemStackSpec lockedDisplayItem,
    List<ChallengeUnlockRequirement> unlockRequirements,
    List<CompletionRequirement> completionRequirements,
    ChallengeProperties properties,
    RepeatPolicy repeatPolicy,
    RewardBundle firstCompletionReward,
    RewardBundle repeatReward
) {
    public ChallengeDefinition {
        id = Objects.requireNonNull(id, "id");
        display = Objects.requireNonNull(display, "display");
        lockedDisplayItem = Objects.requireNonNull(lockedDisplayItem, "lockedDisplayItem");
        unlockRequirements = List.copyOf(Objects.requireNonNull(unlockRequirements, "unlockRequirements"));
        completionRequirements = List.copyOf(Objects.requireNonNull(completionRequirements, "completionRequirements"));
        properties = Objects.requireNonNull(properties, "properties");
        repeatPolicy = Objects.requireNonNull(repeatPolicy, "repeatPolicy");
        firstCompletionReward = Objects.requireNonNull(firstCompletionReward, "firstCompletionReward");
        repeatReward = Objects.requireNonNull(repeatReward, "repeatReward");
    }
}
