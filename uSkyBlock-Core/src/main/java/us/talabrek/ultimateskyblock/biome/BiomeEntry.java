package us.talabrek.ultimateskyblock.biome;

import org.bukkit.block.Biome;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public record BiomeEntry(
    @NotNull ItemStack displayItem,
    @NotNull Biome biome,
    @NotNull String name,
    @NotNull String description
) {

    public BiomeEntry {
        displayItem = displayItem.clone();
    }

    @Override
    public @NotNull ItemStack displayItem() {
        return displayItem.clone();
    }
}
