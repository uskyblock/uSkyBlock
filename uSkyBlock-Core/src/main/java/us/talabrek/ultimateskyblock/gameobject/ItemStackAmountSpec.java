package us.talabrek.ultimateskyblock.gameobject;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class ItemStackAmountSpec {
    private final ItemStackSpec item;
    private final int amount;

    public ItemStackAmountSpec(@NotNull ItemStackSpec item, int amount) {
        this.item = item;
        this.amount = amount;
    }

    @NotNull
    public ItemStackSpec prototype() {
        return item;
    }

    public int amount() {
        return amount;
    }

    @NotNull
    public java.util.List<ItemStack> stacks() {
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        int remaining = amount;
        while (remaining > 0) {
            ItemStack itemStack = item.create();
            int stackAmount = Math.min(remaining, itemStack.getMaxStackSize());
            itemStack.setAmount(stackAmount);
            remaining -= stackAmount;
            items.add(itemStack);
        }
        return java.util.List.copyOf(items);
    }
}
