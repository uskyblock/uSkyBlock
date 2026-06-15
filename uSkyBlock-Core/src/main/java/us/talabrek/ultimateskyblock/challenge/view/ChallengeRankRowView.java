package us.talabrek.ultimateskyblock.challenge.view;

import us.talabrek.ultimateskyblock.challenge.catalog.RankDefinition;

import java.util.List;
import java.util.Objects;

public record ChallengeRankRowView(
    int rowIndex,
    RankDefinition rank,
    boolean rankUnlocked,
    List<ChallengeSlotView> slots
) {
    public ChallengeRankRowView {
        if (rowIndex < 0) {
            throw new IllegalArgumentException("rowIndex cannot be negative");
        }
        rank = Objects.requireNonNull(rank, "rank");
        slots = List.copyOf(Objects.requireNonNull(slots, "slots"));
    }
}
