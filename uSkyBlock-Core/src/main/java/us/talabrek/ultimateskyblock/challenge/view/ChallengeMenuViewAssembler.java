package us.talabrek.ultimateskyblock.challenge.view;

import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeCatalog;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.RankDefinition;
import us.talabrek.ultimateskyblock.gameobject.ItemStackSpec;
import us.talabrek.ultimateskyblock.gui.PaginationBars;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ChallengeMenuViewAssembler {
    public ChallengePageView assemblePage(
        ChallengeCatalog catalog,
        ChallengePresentationState presentationState,
        int pageNumber
    ) {
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(presentationState, "presentationState");
        int totalPages = catalog.ranks().size();
        if (pageNumber < 1 || pageNumber > totalPages) {
            throw new IllegalArgumentException("pageNumber must be between 1 and " + totalPages);
        }

        RankDefinition rank = catalog.ranks().get(pageNumber - 1);
        boolean rankUnlocked = presentationState.isRankUnlocked(rank.id());
        List<ChallengeSlotView> slots = new ArrayList<>();
        for (int i = 0; i < rank.challenges().size(); i++) {
            ChallengeDefinition challenge = rank.challenges().get(i);
            slots.add(assembleSlot(i, rank, rankUnlocked, challenge, presentationState));
        }

        return new ChallengePageView(
            pageNumber,
            totalPages,
            rank,
            rankUnlocked,
            slots,
            PaginationBars.nineSlotBar(pageNumber, totalPages)
        );
    }

    private ChallengeSlotView assembleSlot(
        int slotIndex,
        RankDefinition rank,
        boolean rankUnlocked,
        ChallengeDefinition challenge,
        ChallengePresentationState presentationState
    ) {
        if (!rankUnlocked) {
            return new ChallengeSlotView(
                slotIndex,
                challenge,
                ChallengeSlotState.RANK_LOCKED,
                ChallengeSlotDetailMode.RANK_UNLOCK_DETAILS,
                rank.lockedDisplayItem(),
                false
            );
        }

        if (!presentationState.isChallengeUnlocked(challenge.id())) {
            return new ChallengeSlotView(
                slotIndex,
                challenge,
                ChallengeSlotState.CHALLENGE_LOCKED,
                ChallengeSlotDetailMode.CHALLENGE_UNLOCK_DETAILS,
                challenge.lockedDisplayItem(),
                false
            );
        }

        return new ChallengeSlotView(
            slotIndex,
            challenge,
            ChallengeSlotState.CHALLENGE_UNLOCKED,
            ChallengeSlotDetailMode.CHALLENGE_DETAILS,
            challenge.display().displayItem(),
            true
        );
    }
}
