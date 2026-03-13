package us.talabrek.ultimateskyblock.gameobject;

import dk.lockfuglsang.minecraft.util.FormatUtil;
import dk.lockfuglsang.minecraft.util.ItemStackUtil;
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
        ItemStackUtil.Builder builder = ItemStackUtil.builder(prototype);
        if (name != null) {
            builder.displayName(FormatUtil.normalize(name));
        }
        if (description != null) {
            builder.lore(List.copyOf(FormatUtil.wordWrap(FormatUtil.normalize(description), 30, 30)));
        }
        return builder.build();
    }
}
