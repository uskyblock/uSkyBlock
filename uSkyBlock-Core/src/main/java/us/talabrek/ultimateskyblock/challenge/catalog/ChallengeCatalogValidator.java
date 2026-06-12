package us.talabrek.ultimateskyblock.challenge.catalog;

import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.ChallengeUnlockRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.CompletedChallengesRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.CompletedRankRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.CompletionRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.EntityPresenceRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.InventoryItemsRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.IslandBlocksRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.IslandLevelRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.RankUnlockRequirement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ChallengeCatalogValidator {
    public List<ChallengeCatalogDiagnostic> validate(ChallengeCatalog catalog) {
        List<ChallengeCatalogDiagnostic> diagnostics = new ArrayList<>();
        for (RankDefinition rank : catalog.ranks()) {
            String rankPath = "$.ranks." + rank.id().value();
            if (rank.challenges().isEmpty()) {
                diagnostics.add(warn(rankPath + ".challenges", "Rank defines no challenges"));
            }
            if (rank.challenges().size() > 9) {
                diagnostics.add(warn(rankPath + ".challenges", "Rank has " + rank.challenges().size()
                    + " challenges; the challenge menu displays at most 9 per rank"));
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
        validateCompletionKinds(challenge, challengePath, diagnostics);
    }

    private void validateCompletionKinds(
        ChallengeDefinition challenge,
        String challengePath,
        List<ChallengeCatalogDiagnostic> diagnostics
    ) {
        Set<String> kinds = new HashSet<>();
        for (CompletionRequirement requirement : challenge.completionRequirements()) {
            if (requirement instanceof InventoryItemsRequirement) {
                kinds.add("inventory-items");
            } else if (requirement instanceof IslandBlocksRequirement || requirement instanceof EntityPresenceRequirement) {
                kinds.add("island-blocks/entity-presence");
            } else if (requirement instanceof IslandLevelRequirement) {
                kinds.add("island-level");
            }
        }
        if (kinds.size() > 1) {
            diagnostics.add(warn(challengePath + ".complete",
                "Mixes completion requirement kinds (" + String.join(", ", kinds)
                    + "); the challenge menu cannot display it until the new menu ships"));
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
            var referencedRank = catalog.rank(completedRankRequirement.rankId());
            if (referencedRank.isEmpty()) {
                diagnostics.add(warn(path, "References unknown rank '" + completedRankRequirement.rankId().value() + "'"));
            } else if (completedRankRequirement.minimumCompletedChallenges() > referencedRank.get().challenges().size()) {
                diagnostics.add(warn(path, "Requires " + completedRankRequirement.minimumCompletedChallenges()
                    + " completed challenges, but rank '" + completedRankRequirement.rankId().value()
                    + "' only has " + referencedRank.get().challenges().size()));
            }
        }
    }

    private boolean hasNoInventoryHandIn(List<CompletionRequirement> requirements) {
        return requirements.stream().noneMatch(InventoryItemsRequirement.class::isInstance);
    }

    private static ChallengeCatalogDiagnostic warn(String path, String message) {
        return new ChallengeCatalogDiagnostic(ChallengeCatalogDiagnostic.Severity.WARNING, path, message);
    }

    private static ChallengeCatalogDiagnostic error(String path, String message) {
        return new ChallengeCatalogDiagnostic(ChallengeCatalogDiagnostic.Severity.ERROR, path, message);
    }
}
