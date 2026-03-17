package us.talabrek.ultimateskyblock.challenge.catalog;

import java.util.Objects;

public record RankId(String value) {
    public RankId {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Rank id cannot be blank");
        }
    }

    public static RankId of(String value) {
        return new RankId(value);
    }
}
