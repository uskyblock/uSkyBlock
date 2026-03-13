package us.talabrek.ultimateskyblock.biome;

import org.bukkit.block.Biome;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.gameobject.ItemStackSpec;

public final class BiomeEntry {
    private final ItemStackSpec displayItem;
    private final Biome biome;
    private final String name;
    private final String description;

    public BiomeEntry(
        @NotNull ItemStackSpec displayItem,
        @NotNull Biome biome,
        @NotNull String name,
        @NotNull String description
    ) {
        this.displayItem = displayItem;
        this.biome = biome;
        this.name = name;
        this.description = description;
    }

    public @NotNull ItemStack displayItem() {
        return displayItem.create(name, description);
    }

    public @NotNull Biome biome() {
        return biome;
    }

    public @NotNull String name() {
        return name;
    }

    public @NotNull String description() {
        return description;
    }
}
