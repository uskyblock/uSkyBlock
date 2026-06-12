package us.talabrek.ultimateskyblock.challenge.view;

import net.kyori.adventure.text.Component;
import us.talabrek.ultimateskyblock.challenge.ChallengeCompletion;
import us.talabrek.ultimateskyblock.challenge.ChallengeKey;
import us.talabrek.ultimateskyblock.challenge.ChallengeText;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeCatalog;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.RankDefinition;
import us.talabrek.ultimateskyblock.gui.PaginationBars;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static us.talabrek.ultimateskyblock.message.Msg.ERROR;
import static us.talabrek.ultimateskyblock.message.Msg.MUTED;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Msg.SECONDARY;
import static us.talabrek.ultimateskyblock.message.Placeholder.component;

public final class ChallengeMenuViewAssembler {
    public static final int RANKS_PER_PAGE = 5;
    public static final int CHALLENGES_PER_RANK_ROW = 9;

    public ChallengePageView assemblePage(
        ChallengeCatalog catalog,
        ChallengePresentationState presentationState,
        Map<ChallengeKey, ChallengeCompletion> progress,
        boolean economyEnabled,
        int pageNumber
    ) {
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(presentationState, "presentationState");
        Objects.requireNonNull(progress, "progress");
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
            // Anything beyond the row is reported by the catalog validator and not rendered.
            int columns = Math.min(rank.challenges().size(), CHALLENGES_PER_RANK_ROW);
            for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
                ChallengeDefinition challenge = rank.challenges().get(columnIndex);
                slots.add(assembleSlot(rowIndex, columnIndex, rank, rankUnlocked, challenge, presentationState, progress, economyEnabled));
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
        ChallengePresentationState presentationState,
        Map<ChallengeKey, ChallengeCompletion> progress,
        boolean economyEnabled
    ) {
        int slotIndex = rowIndex * CHALLENGES_PER_RANK_ROW + columnIndex;
        ChallengeCompletion completion = progress.get(ChallengeKey.of(challenge.id().value()));
        boolean completed = completion != null && completion.getTimesCompleted() > 0;
        if (!rankUnlocked) {
            return new ChallengeSlotView(
                slotIndex,
                rank,
                challenge,
                ChallengeSlotState.RANK_LOCKED,
                rank.lockedDisplayItem(),
                tr("Locked Rank: <rank>", ERROR, component("rank", ChallengeText.displayName(rank))),
                rankLockedLore(rank),
                false,
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
                tr("Locked Challenge", ERROR),
                challengeLockedLore(rank, challenge),
                false,
                false
            );
        }

        return new ChallengeSlotView(
            slotIndex,
            rank,
            challenge,
            ChallengeSlotState.CHALLENGE_UNLOCKED,
            challenge.display().displayItem(),
            ChallengeText.displayName(challenge).applyFallbackStyle(completed ? SECONDARY : PRIMARY),
            challengeLore(rank, challenge, completion, economyEnabled),
            true,
            completed
        );
    }

    private List<Component> rankLockedLore(RankDefinition rank) {
        List<Component> lore = baseRankLore(rank);
        lore.add(Component.empty());
        lore.add(tr("Unlock this rank to access its challenges.", ERROR));
        return List.copyOf(lore);
    }

    private List<Component> challengeLockedLore(RankDefinition rank, ChallengeDefinition challenge) {
        List<Component> lore = baseRankLore(rank);
        lore.add(Component.empty());
        lore.add(ChallengeText.displayName(challenge).applyFallbackStyle(PRIMARY));
        lore.add(tr("Unlock this challenge to continue.", ERROR));
        return List.copyOf(lore);
    }

    private List<Component> challengeLore(
        RankDefinition rank,
        ChallengeDefinition challenge,
        ChallengeCompletion completion,
        boolean economyEnabled
    ) {
        List<Component> lore = new ArrayList<>();
        lore.add(tr("Rank: <rank>", MUTED, component("rank", ChallengeText.displayName(rank))));
        lore.addAll(ChallengeLore.describe(challenge, completion, economyEnabled));
        lore.add(Component.empty());
        lore.add(tr("Click to attempt this challenge.", PRIMARY));
        return List.copyOf(lore);
    }

    private List<Component> baseRankLore(RankDefinition rank) {
        List<Component> lore = new ArrayList<>();
        lore.add(ChallengeText.displayName(rank).applyFallbackStyle(MUTED));
        String description = rank.display().description().source();
        if (!description.isBlank()) {
            lore.add(ChallengeText.render(rank.display().description()).applyFallbackStyle(MUTED));
        }
        return lore;
    }
}
