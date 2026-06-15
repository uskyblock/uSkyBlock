package us.talabrek.ultimateskyblock.challenge.catalog;

import java.util.Objects;

public record RankDisplaySpec(TextSpec name, TextSpec description) {
    public RankDisplaySpec {
        name = Objects.requireNonNull(name, "name");
        description = Objects.requireNonNull(description, "description");
    }
}
