package us.talabrek.ultimateskyblock.challenge.catalog;

import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.RankUnlockRequirement;
import us.talabrek.ultimateskyblock.gameobject.ItemStackSpec;

import java.util.List;
import java.util.Objects;

public record RankDefinition(
    RankId id,
    RankDisplaySpec display,
    ItemStackSpec lockedDisplayItem,
    List<RankUnlockRequirement> unlockRequirements,
    List<ChallengeDefinition> challenges
) {
    public RankDefinition {
        id = Objects.requireNonNull(id, "id");
        display = Objects.requireNonNull(display, "display");
        lockedDisplayItem = Objects.requireNonNull(lockedDisplayItem, "lockedDisplayItem");
        unlockRequirements = List.copyOf(Objects.requireNonNull(unlockRequirements, "unlockRequirements"));
        challenges = List.copyOf(Objects.requireNonNull(challenges, "challenges"));
    }
}
