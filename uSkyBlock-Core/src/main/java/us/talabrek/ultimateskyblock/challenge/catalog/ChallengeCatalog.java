package us.talabrek.ultimateskyblock.challenge.catalog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ChallengeCatalog {
    private final List<RankDefinition> ranks;
    private final ChallengeCatalogIndex index;

    public ChallengeCatalog(List<RankDefinition> ranks) {
        this.ranks = List.copyOf(Objects.requireNonNull(ranks, "ranks"));
        this.index = buildIndex(this.ranks);
    }

    public List<RankDefinition> ranks() {
        return ranks;
    }

    public ChallengeCatalogIndex index() {
        return index;
    }

    public Optional<RankDefinition> rank(RankId id) {
        return Optional.ofNullable(index.ranksById().get(id));
    }

    public Optional<ChallengeDefinition> challenge(ChallengeId id) {
        return Optional.ofNullable(index.challengesById().get(id));
    }

    private static ChallengeCatalogIndex buildIndex(List<RankDefinition> ranks) {
        Map<RankId, RankDefinition> ranksById = new LinkedHashMap<>();
        Map<ChallengeId, ChallengeDefinition> challengesById = new LinkedHashMap<>();
        Map<ChallengeId, RankId> challengeOwners = new LinkedHashMap<>();

        for (RankDefinition rank : ranks) {
            RankDefinition previous = ranksById.putIfAbsent(rank.id(), rank);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate rank id: " + rank.id().value());
            }

            for (ChallengeDefinition challenge : rank.challenges()) {
                ChallengeDefinition previousChallenge = challengesById.putIfAbsent(challenge.id(), challenge);
                if (previousChallenge != null) {
                    throw new IllegalArgumentException("Duplicate challenge id: " + challenge.id().value());
                }
                challengeOwners.put(challenge.id(), rank.id());
            }
        }

        return new ChallengeCatalogIndex(ranksById, challengesById, challengeOwners);
    }
}
