package us.talabrek.ultimateskyblock.challenge.view;

import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeDefinition;
import us.talabrek.ultimateskyblock.gameobject.ItemStackSpec;

import java.util.Objects;

public record ChallengeSlotView(
    int slotIndex,
    ChallengeDefinition challenge,
    ChallengeSlotState state,
    ChallengeSlotDetailMode detailMode,
    ItemStackSpec icon,
    boolean clickable
) {
    public ChallengeSlotView {
        if (slotIndex < 0) {
            throw new IllegalArgumentException("slotIndex cannot be negative");
        }
        challenge = Objects.requireNonNull(challenge, "challenge");
        state = Objects.requireNonNull(state, "state");
        detailMode = Objects.requireNonNull(detailMode, "detailMode");
        icon = Objects.requireNonNull(icon, "icon");
    }
}
