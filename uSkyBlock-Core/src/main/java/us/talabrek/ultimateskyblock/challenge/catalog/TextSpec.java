package us.talabrek.ultimateskyblock.challenge.catalog;

import java.util.Objects;

/**
 * MiniMessage source text for catalog display fields.
 */
public record TextSpec(String source) {
    private static final TextSpec EMPTY = new TextSpec("");

    public TextSpec {
        source = Objects.requireNonNull(source, "source");
    }

    public static TextSpec miniMessage(String source) {
        return new TextSpec(source);
    }

    public static TextSpec empty() {
        return EMPTY;
    }
}
