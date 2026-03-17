package us.talabrek.ultimateskyblock.challenge.catalog;

import us.talabrek.ultimateskyblock.gameobject.ItemStackSpec;

import java.util.Objects;

public record DisplaySpec(TextSpec name, TextSpec description, ItemStackSpec displayItem) {
    public DisplaySpec {
        name = Objects.requireNonNull(name, "name");
        description = Objects.requireNonNull(description, "description");
        displayItem = Objects.requireNonNull(displayItem, "displayItem");
    }
}
