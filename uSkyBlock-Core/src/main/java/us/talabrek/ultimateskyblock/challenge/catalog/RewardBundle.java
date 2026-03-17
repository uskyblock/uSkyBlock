package us.talabrek.ultimateskyblock.challenge.catalog;

import java.util.List;
import java.util.Objects;

public record RewardBundle(List<ChallengeRewards.RewardAction> actions) {
    private static final RewardBundle EMPTY = new RewardBundle(List.of());

    public RewardBundle {
        actions = List.copyOf(Objects.requireNonNull(actions, "actions"));
    }

    public static RewardBundle empty() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return actions.isEmpty();
    }
}
