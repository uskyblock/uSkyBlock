package us.talabrek.ultimateskyblock.challenge.catalog;

import java.util.Objects;

public record TextSpec(String source, Format format) {
    public enum Format {
        MINI_MESSAGE
    }

    private static final TextSpec EMPTY = new TextSpec("", Format.MINI_MESSAGE);

    public TextSpec {
        source = Objects.requireNonNull(source, "source");
        format = Objects.requireNonNull(format, "format");
    }

    public static TextSpec miniMessage(String source) {
        return new TextSpec(source, Format.MINI_MESSAGE);
    }

    public static TextSpec empty() {
        return EMPTY;
    }
}
