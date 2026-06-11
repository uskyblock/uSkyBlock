package us.talabrek.ultimateskyblock.challenge.view;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeCatalog;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.RankDefinition;
import us.talabrek.ultimateskyblock.gui.PaginationBars;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ChallengeMenuViewAssembler {
    public static final int RANKS_PER_PAGE = 5;

    public ChallengePageView assemblePage(
        ChallengeCatalog catalog,
        ChallengePresentationState presentationState,
        int pageNumber
    ) {
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(presentationState, "presentationState");
        int totalPages = Math.max(1, (int) Math.ceil(catalog.ranks().size() / (double) RANKS_PER_PAGE));
        if (pageNumber < 1 || pageNumber > totalPages) {
            throw new IllegalArgumentException("pageNumber must be between 1 and " + totalPages);
        }

        int startIndex = (pageNumber - 1) * RANKS_PER_PAGE;
        int endIndex = Math.min(startIndex + RANKS_PER_PAGE, catalog.ranks().size());
        List<ChallengeRankRowView> rows = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < endIndex - startIndex; rowIndex++) {
            RankDefinition rank = catalog.ranks().get(startIndex + rowIndex);
            boolean rankUnlocked = presentationState.isRankUnlocked(rank.id());
            List<ChallengeSlotView> slots = new ArrayList<>();
            for (int columnIndex = 0; columnIndex < rank.challenges().size(); columnIndex++) {
                ChallengeDefinition challenge = rank.challenges().get(columnIndex);
                slots.add(assembleSlot(rowIndex, columnIndex, rank, rankUnlocked, challenge, presentationState));
            }
            rows.add(new ChallengeRankRowView(rowIndex, rank, rankUnlocked, slots));
        }

        return new ChallengePageView(
            pageNumber,
            totalPages,
            rows,
            PaginationBars.nineSlotBar(pageNumber, totalPages)
        );
    }

    private ChallengeSlotView assembleSlot(
        int rowIndex,
        int columnIndex,
        RankDefinition rank,
        boolean rankUnlocked,
        ChallengeDefinition challenge,
        ChallengePresentationState presentationState
    ) {
        int slotIndex = rowIndex * 9 + columnIndex;
        if (!rankUnlocked) {
            return new ChallengeSlotView(
                slotIndex,
                rank,
                challenge,
                ChallengeSlotState.RANK_LOCKED,
                rank.lockedDisplayItem(),
                rankLockedTitle(rank),
                rankLockedLore(rank),
                false
            );
        }

        if (!presentationState.isChallengeUnlocked(challenge.id())) {
            return new ChallengeSlotView(
                slotIndex,
                rank,
                challenge,
                ChallengeSlotState.CHALLENGE_LOCKED,
                challenge.lockedDisplayItem(),
                challengeLockedTitle(challenge),
                challengeLockedLore(rank, challenge),
                false
            );
        }

        return new ChallengeSlotView(
            slotIndex,
            rank,
            challenge,
            ChallengeSlotState.CHALLENGE_UNLOCKED,
            challenge.display().displayItem(),
            challengeTitle(challenge),
            challengeLore(rank, challenge),
            true
        );
    }

    private Component rankLockedTitle(RankDefinition rank) {
        return Component.text(rank.display().name().source(), NamedTextColor.RED);
    }

    private List<Component> rankLockedLore(RankDefinition rank) {
        List<Component> lore = baseRankLore(rank);
        lore.add(Component.empty());
        lore.add(Component.text("Unlock this rank to access its challenges.", NamedTextColor.RED));
        return List.copyOf(lore);
    }

    private Component challengeLockedTitle(ChallengeDefinition challenge) {
        return Component.text(challenge.display().name().source(), NamedTextColor.RED);
    }

    private List<Component> challengeLockedLore(RankDefinition rank, ChallengeDefinition challenge) {
        List<Component> lore = baseRankLore(rank);
        lore.add(Component.empty());
        lore.add(Component.text(challenge.display().name().source(), NamedTextColor.YELLOW));
        appendDescription(lore, challenge.display().description().source());
        lore.add(Component.text("Unlock this challenge to continue.", NamedTextColor.RED));
        return List.copyOf(lore);
    }

    private Component challengeTitle(ChallengeDefinition challenge) {
        return Component.text(challenge.display().name().source(), NamedTextColor.GREEN);
    }

    private List<Component> challengeLore(RankDefinition rank, ChallengeDefinition challenge) {
        List<Component> lore = baseRankLore(rank);
        lore.add(Component.empty());
        lore.add(Component.text(challenge.display().name().source(), NamedTextColor.GREEN));
        appendDescription(lore, challenge.display().description().source());
        lore.add(Component.text("Click to attempt this challenge.", NamedTextColor.AQUA));
        return List.copyOf(lore);
    }

    private List<Component> baseRankLore(RankDefinition rank) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(rank.display().name().source(), NamedTextColor.GOLD));
        appendDescription(lore, rank.display().description().source());
        return lore;
    }

    private void appendDescription(List<Component> lore, String description) {
        if (!description.isBlank()) {
            lore.add(Component.text(description, NamedTextColor.GRAY));
        }
    }
}
