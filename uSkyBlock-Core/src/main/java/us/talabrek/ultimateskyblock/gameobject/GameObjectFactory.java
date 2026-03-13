package us.talabrek.ultimateskyblock.gameobject;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Parses reusable gameplay-object definitions from config and challenge files.
 *
 * <p>Parsing currently delegates to the existing {@link ItemStackUtil} helpers so the
 * behavior stays aligned while the value objects in core become more explicit.</p>
 */
@Singleton
public class GameObjectFactory {
    @Inject
    public GameObjectFactory() {
    }

    @NotNull
    public ItemStackSpec itemStack(@NotNull String specification) {
        return new ItemStackSpec(ItemStackUtil.createItemStack(specification));
    }

    @NotNull
    public ItemStackAmountSpec itemStackAmount(@NotNull String specification) {
        return fromItemStackWithAmount(single(ItemStackUtil.createItemList(List.of(specification))));
    }

    @NotNull
    public List<ItemStackAmountSpec> itemStackAmounts(@NotNull List<String> specifications) {
        return specifications.stream()
            .map(this::itemStackAmount)
            .toList();
    }

    @NotNull
    public ItemStackAmountProbabilitySpec itemStackAmountProbability(@NotNull String specification) {
        var parsed = single(ItemStackUtil.createItemsWithProbability(List.of(specification)));
        return new ItemStackAmountProbabilitySpec(parsed.probability(), fromItemStackWithAmount(parsed.item()));
    }

    @NotNull
    public List<ItemStackAmountProbabilitySpec> itemStackAmountProbabilities(@NotNull List<String> specifications) {
        return specifications.stream()
            .map(this::itemStackAmountProbability)
            .toList();
    }

    @NotNull
    private ItemStackAmountSpec fromItemStackWithAmount(@NotNull ItemStack itemStack) {
        ItemStack copy = itemStack.clone();
        int amount = copy.getAmount();
        copy.setAmount(1);
        return new ItemStackAmountSpec(new ItemStackSpec(copy), amount);
    }

    @NotNull
    private static <T> T single(@NotNull List<T> values) {
        if (values.size() != 1) {
            throw new IllegalArgumentException("Expected a single parsed value, but got " + values.size());
        }
        return values.get(0);
    }
}
