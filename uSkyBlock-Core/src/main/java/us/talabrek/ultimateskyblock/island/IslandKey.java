package us.talabrek.ultimateskyblock.island;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Canonical storage identifier for an island.
 */
public record IslandKey(String value) {
    private static final Pattern ISLAND_KEY_PATTERN = Pattern.compile("-?\\d+,-?\\d+");

    public IslandKey {
        value = Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Island key cannot be blank");
        }
        if (!ISLAND_KEY_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid island key: " + value);
        }
    }

    public static @NotNull IslandKey fromIslandName(@NotNull String islandName) {
        return new IslandKey(islandName);
    }
}
