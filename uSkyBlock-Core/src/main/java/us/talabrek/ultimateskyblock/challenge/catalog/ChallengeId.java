package us.talabrek.ultimateskyblock.challenge.catalog;

import java.util.Locale;
import java.util.Objects;

public record ChallengeId(String value) {
    public ChallengeId {
        value = Objects.requireNonNull(value, "value").trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Challenge id cannot be blank");
        }
    }

    public static ChallengeId of(String value) {
        return new ChallengeId(value);
    }
}
