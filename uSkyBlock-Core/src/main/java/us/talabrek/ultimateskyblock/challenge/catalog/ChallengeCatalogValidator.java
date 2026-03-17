package us.talabrek.ultimateskyblock.challenge.catalog;

import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.ChallengeUnlockRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.CompletedChallengesRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.CompletedRankRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.CompletionRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.InventoryItemsRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.RankUnlockRequirement;

import java.util.ArrayList;
import java.util.List;

public final class ChallengeCatalogValidator {
    public List<ChallengeCatalogDiagnostic> validate(ChallengeCatalog catalog) {
        List<ChallengeCatalogDiagnostic> diagnostics = new ArrayList<>();
        for (RankDefinition rank : catalog.ranks()) {
            String rankPath = "$.ranks." + rank.id().value();
            if (rank.challenges().isEmpty()) {
                diagnostics.add(warn(rankPath + ".challenges", "Rank defines no challenges"));
            }
            validateRankUnlockRequirements(rank.unlockRequirements(), rankPath + ".unlock", catalog, diagnostics);
            for (ChallengeDefinition challenge : rank.challenges()) {
                validateChallenge(rank, challenge, catalog, diagnostics);
            }
        }
        return diagnostics;
    }

    private void validateChallenge(
        RankDefinition rank,
        ChallengeDefinition challenge,
        ChallengeCatalog catalog,
        List<ChallengeCatalogDiagnostic> diagnostics
    ) {
        String challengePath = "$.ranks." + rank.id().value() + ".challenges." + challenge.id().value();
        validateChallengeUnlockRequirements(challenge.unlockRequirements(), challengePath + ".unlock", catalog, diagnostics);

        if (challenge.completionRequirements().isEmpty()) {
            diagnostics.add(warn(challengePath + ".complete", "Challenge has no completion requirements"));
        }
        if (challenge.firstCompletionReward().isEmpty() && challenge.repeatReward().isEmpty()) {
            diagnostics.add(warn(challengePath + ".rewards", "Challenge has no rewards"));
        }
        if (!challenge.repeatPolicy().repeatable() && !challenge.repeatReward().isEmpty()) {
            diagnostics.add(warn(challengePath + ".rewards.repeat", "Repeat reward is configured, but repeat is disabled"));
        }
        if (challenge.repeatPolicy().repeatable() && hasNoInventoryHandIn(challenge.completionRequirements())) {
            diagnostics.add(warn(challengePath + ".repeat", "Repeatable challenge has no inventory hand-in requirement"));
        }
    }

    private void validateRankUnlockRequirements(
        List<RankUnlockRequirement> requirements,
        String path,
        ChallengeCatalog catalog,
        List<ChallengeCatalogDiagnostic> diagnostics
    ) {
        for (int i = 0; i < requirements.size(); i++) {
            validateRequirementReference(requirements.get(i), path + "[" + i + "]", catalog, diagnostics);
        }
    }

    private void validateChallengeUnlockRequirements(
        List<ChallengeUnlockRequirement> requirements,
        String path,
        ChallengeCatalog catalog,
        List<ChallengeCatalogDiagnostic> diagnostics
    ) {
        for (int i = 0; i < requirements.size(); i++) {
            validateRequirementReference(requirements.get(i), path + "[" + i + "]", catalog, diagnostics);
        }
    }

    private void validateRequirementReference(
        Object requirement,
        String path,
        ChallengeCatalog catalog,
        List<ChallengeCatalogDiagnostic> diagnostics
    ) {
        if (requirement instanceof CompletedChallengesRequirement completedChallengesRequirement) {
            for (ChallengeId challengeId : completedChallengesRequirement.challengeIds()) {
                if (catalog.challenge(challengeId).isEmpty()) {
                    diagnostics.add(warn(path, "References unknown challenge '" + challengeId.value() + "'"));
                }
            }
        } else if (requirement instanceof CompletedRankRequirement completedRankRequirement) {
            if (catalog.rank(completedRankRequirement.rankId()).isEmpty()) {
                diagnostics.add(warn(path, "References unknown rank '" + completedRankRequirement.rankId().value() + "'"));
            }
        }
    }

    private boolean hasNoInventoryHandIn(List<CompletionRequirement> requirements) {
        return requirements.stream().noneMatch(InventoryItemsRequirement.class::isInstance);
    }

    private static ChallengeCatalogDiagnostic warn(String path, String message) {
        return new ChallengeCatalogDiagnostic(ChallengeCatalogDiagnostic.Severity.WARNING, path, message);
    }
}
