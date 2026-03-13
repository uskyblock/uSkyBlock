package us.talabrek.ultimateskyblock.gameobject;

import dk.lockfuglsang.minecraft.util.FormatUtil;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Parsed item prototype that always returns a clone on access.
 */
public final class ItemStackSpec {
    private final ItemStack prototype;

    public ItemStackSpec(@NotNull ItemStack prototype) {
        this.prototype = prototype.clone();
        this.prototype.setAmount(1);
    }

    @NotNull
    public ItemStack create() {
        return prototype.clone();
    }

    @NotNull
    public ItemStack create(@Nullable String name, @Nullable String description) {
        ItemStack itemStack = prototype.clone();
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return itemStack;
        }
        if (name != null) {
            itemMeta.setDisplayName(FormatUtil.normalize(name));
        }
        if (description != null) {
            itemMeta.setLore(List.copyOf(FormatUtil.wordWrap(FormatUtil.normalize(description), 30, 30)));
        } else {
            itemMeta.setLore(List.of());
        }
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}
