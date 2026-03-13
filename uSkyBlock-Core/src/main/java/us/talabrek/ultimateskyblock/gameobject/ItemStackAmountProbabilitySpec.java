package us.talabrek.ultimateskyblock.gameobject;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class ItemStackAmountProbabilitySpec {
    private final double probability;
    private final ItemStackAmountSpec item;

    public ItemStackAmountProbabilitySpec(double probability, @NotNull ItemStackAmountSpec item) {
        this.probability = probability;
        this.item = item;
    }

    public double probability() {
        return probability;
    }

    @NotNull
    public ItemStackAmountSpec item() {
        return item;
    }
}
