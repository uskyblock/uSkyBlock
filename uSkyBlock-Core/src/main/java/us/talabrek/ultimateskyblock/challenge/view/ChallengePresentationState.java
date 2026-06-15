package us.talabrek.ultimateskyblock.challenge.view;

import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeId;
import us.talabrek.ultimateskyblock.challenge.catalog.RankId;

public interface ChallengePresentationState {
    boolean isRankUnlocked(RankId rankId);

    boolean isChallengeUnlocked(ChallengeId challengeId);

    static ChallengePresentationState allUnlocked() {
        return new ChallengePresentationSnapshot(java.util.Set.of(), java.util.Set.of(), true);
    }
}
