package us.talabrek.ultimateskyblock.challenge.view;

import us.talabrek.ultimateskyblock.challenge.catalog.RankDefinition;
import us.talabrek.ultimateskyblock.gui.PaginationBar;

import java.util.List;
import java.util.Objects;

public record ChallengePageView(
    int pageNumber,
    int totalPages,
    RankDefinition rank,
    boolean rankUnlocked,
    List<ChallengeSlotView> slots,
    PaginationBar pagination
) {
    public ChallengePageView {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be positive");
        }
        if (totalPages < 1) {
            throw new IllegalArgumentException("totalPages must be positive");
        }
        if (pageNumber > totalPages) {
            throw new IllegalArgumentException("pageNumber cannot exceed totalPages");
        }
        rank = Objects.requireNonNull(rank, "rank");
        slots = List.copyOf(Objects.requireNonNull(slots, "slots"));
        pagination = Objects.requireNonNull(pagination, "pagination");
    }
}
