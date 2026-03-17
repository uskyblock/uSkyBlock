package us.talabrek.ultimateskyblock.challenge.catalog;

import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.RankUnlockRequirement;

import java.util.List;
import java.util.Objects;

public record RankDefinition(
    RankId id,
    DisplaySpec display,
    List<RankUnlockRequirement> unlockRequirements,
    List<ChallengeDefinition> challenges
) {
    public RankDefinition {
        id = Objects.requireNonNull(id, "id");
        display = Objects.requireNonNull(display, "display");
        unlockRequirements = List.copyOf(Objects.requireNonNull(unlockRequirements, "unlockRequirements"));
        challenges = List.copyOf(Objects.requireNonNull(challenges, "challenges"));
    }
}
