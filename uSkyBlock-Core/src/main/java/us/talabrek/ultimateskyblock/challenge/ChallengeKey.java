package us.talabrek.ultimateskyblock.challenge;

import java.util.Locale;
import java.util.Objects;

/**
 * Lightweight, value-based wrapper for a canonical challenge id.
 * <p>
 * The id corresponds to the internal challenge name (e.g., "cobblestonegenerator").
 * This type is intended for unambiguous operations where the caller already
 * knows the exact identifier and no fuzzy resolution should take place.
 */
public record ChallengeKey(String id) {

    public ChallengeKey {
        id = Objects.requireNonNull(id, "id cannot be null").toLowerCase(Locale.ROOT);
    }

    public static ChallengeKey of(String id) {
        return new ChallengeKey(id);
    }
}
