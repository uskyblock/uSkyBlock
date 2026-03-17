package us.talabrek.ultimateskyblock.challenge.view;

import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeId;
import us.talabrek.ultimateskyblock.challenge.catalog.RankId;

import java.util.Objects;
import java.util.Set;

public record ChallengePresentationSnapshot(
    Set<RankId> unlockedRanks,
    Set<ChallengeId> unlockedChallenges,
    boolean allUnlocked
) implements ChallengePresentationState {
    public ChallengePresentationSnapshot(Set<RankId> unlockedRanks, Set<ChallengeId> unlockedChallenges) {
        this(unlockedRanks, unlockedChallenges, false);
    }

    public ChallengePresentationSnapshot {
        unlockedRanks = Set.copyOf(Objects.requireNonNull(unlockedRanks, "unlockedRanks"));
        unlockedChallenges = Set.copyOf(Objects.requireNonNull(unlockedChallenges, "unlockedChallenges"));
    }

    @Override
    public boolean isRankUnlocked(RankId rankId) {
        return allUnlocked || unlockedRanks.contains(rankId);
    }

    @Override
    public boolean isChallengeUnlocked(ChallengeId challengeId) {
        return allUnlocked || unlockedChallenges.contains(challengeId);
    }
}
