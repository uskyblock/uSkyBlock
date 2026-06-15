package us.talabrek.ultimateskyblock.challenge.catalog;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ChallengeCatalogIndex {
    private final Map<RankId, RankDefinition> ranksById;
    private final Map<ChallengeId, ChallengeDefinition> challengesById;
    private final Map<ChallengeId, RankId> challengeOwners;

    ChallengeCatalogIndex(
        Map<RankId, RankDefinition> ranksById,
        Map<ChallengeId, ChallengeDefinition> challengesById,
        Map<ChallengeId, RankId> challengeOwners
    ) {
        this.ranksById = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(ranksById, "ranksById")));
        this.challengesById = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(challengesById, "challengesById")));
        this.challengeOwners = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(challengeOwners, "challengeOwners")));
    }

    public Map<RankId, RankDefinition> ranksById() {
        return ranksById;
    }

    public Map<ChallengeId, ChallengeDefinition> challengesById() {
        return challengesById;
    }

    public Map<ChallengeId, RankId> challengeOwners() {
        return challengeOwners;
    }
}
