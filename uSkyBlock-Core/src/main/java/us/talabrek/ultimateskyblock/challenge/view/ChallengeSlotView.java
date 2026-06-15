package us.talabrek.ultimateskyblock.challenge.view;

import net.kyori.adventure.text.Component;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.RankDefinition;
import us.talabrek.ultimateskyblock.gameobject.ItemStackSpec;

import java.util.List;
import java.util.Objects;

public record ChallengeSlotView(
    int slotIndex,
    RankDefinition rank,
    ChallengeDefinition challenge,
    ChallengeSlotState state,
    ItemStackSpec icon,
    Component title,
    List<Component> lore,
    boolean clickable,
    boolean completed
) {
    public ChallengeSlotView {
        if (slotIndex < 0) {
            throw new IllegalArgumentException("slotIndex cannot be negative");
        }
        rank = Objects.requireNonNull(rank, "rank");
        challenge = Objects.requireNonNull(challenge, "challenge");
        state = Objects.requireNonNull(state, "state");
        icon = Objects.requireNonNull(icon, "icon");
        title = Objects.requireNonNull(title, "title");
        lore = List.copyOf(Objects.requireNonNull(lore, "lore"));
    }
}
