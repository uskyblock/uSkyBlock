package us.talabrek.ultimateskyblock.challenge.catalog;

import java.time.Duration;
import java.util.Objects;

public record RepeatPolicy(boolean repeatable, Duration resetWindow, int repeatLimit) {
    public RepeatPolicy {
        resetWindow = Objects.requireNonNull(resetWindow, "resetWindow");
        if (resetWindow.isNegative()) {
            throw new IllegalArgumentException("resetWindow cannot be negative");
        }
        if (repeatLimit < 0) {
            throw new IllegalArgumentException("repeatLimit cannot be negative");
        }
    }

    public boolean isUnlimited() {
        return repeatLimit == 0;
    }
}
