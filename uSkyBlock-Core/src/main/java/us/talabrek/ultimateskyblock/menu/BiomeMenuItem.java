package us.talabrek.ultimateskyblock.menu;

import org.bukkit.block.Biome;
import org.bukkit.inventory.ItemStack;

public class BiomeMenuItem {
    private final ItemStack icon;
    private final Biome biome;
    private final String title;
    private final String description;

    public BiomeMenuItem(ItemStack icon, Biome biome, String title, String description) {
        this.icon = icon;
        this.biome = biome;
        this.title = title;
        this.description = description;
    }

    public ItemStack getIcon() {
        return icon.clone();
    }

    public Biome getBiome() {
        return biome;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
