package dk.lockfuglsang.minecraft.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

/**
 * Conversion to ItemStack from strings.
 */
public enum ItemStackUtil {
    ;
    private static final Pattern ITEM_AMOUNT_PROBABILITY_PATTERN = Pattern.compile(
        "(\\{p=(?<prob>0\\.[0-9]+)})?(?<type>(minecraft:)?[0-9A-Za-z_]+(\\[.*])?):(?<amount>[0-9]+)"
    );
    private static final Pattern ITEM_TYPE_PATTERN = Pattern.compile(
        "(?<type>(minecraft:)?[0-9A-Za-z_]+(\\[.*])?)"
    );

    @NotNull
    public static List<ItemProbability> createItemsWithProbability(@NotNull List<String> items) {
        List<ItemProbability> itemsWithProbability = new ArrayList<>();
        for (String reward : items) {
            Matcher matcher = ITEM_AMOUNT_PROBABILITY_PATTERN.matcher(reward);
            if (matcher.matches()) {
                double probability = matcher.group("prob") != null ?
                    Double.parseDouble(matcher.group("prob")) : 1.0;
                ItemStack itemStack = getItemType(matcher);
                int amount = Integer.parseInt(matcher.group("amount"));
                itemStack.setAmount(amount);
                itemsWithProbability.add(new ItemProbability(probability, itemStack));
            } else {
                throw new IllegalArgumentException("Unknown item: '" + reward + "' in '" + items + "'");
            }
        }
        return itemsWithProbability;
    }

    @NotNull
    private static ItemStack getItemType(@NotNull Matcher matcher) {
        String type = matcher.group("type");
        return Bukkit.getItemFactory().createItemStack(type.toLowerCase(Locale.ROOT));
    }

    @NotNull
    public static List<ItemStack> createItemList(@NotNull List<String> items) {
        List<ItemStack> itemList = new ArrayList<>();
        for (String reward : items) {
            if (reward != null && !reward.isEmpty()) {
                itemList.add(createItemStackAmount(reward));
            }
        }
        return itemList;
    }

    @NotNull
    private static ItemStack createItemStackAmount(@NotNull String reward) {
        Matcher matcher = ITEM_AMOUNT_PROBABILITY_PATTERN.matcher(reward);
        if (matcher.matches()) {
            ItemStack itemStack = getItemType(matcher);
            int amount = Integer.parseInt(matcher.group("amount"));
            itemStack.setAmount(amount);
            return itemStack;
        } else {
            throw new IllegalArgumentException("Unknown item: '" + reward + "'");
        }
    }

    public static ItemStack[] createItemArray(List<ItemStack> items) {
        return items != null ? items.toArray(new ItemStack[0]) : new ItemStack[0];
    }

    public static ItemStack createItemStack(String displayItem) {
        return createItemStack(displayItem, null, null);
    }

    public static ItemStack createItemStack(@NotNull String displayItem, @Nullable String name, @Nullable String description) {
        Matcher matcher = ITEM_TYPE_PATTERN.matcher(displayItem);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid item " + displayItem + " supplied!");
        }
        ItemStack itemStack = getItemType(matcher);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(FormatUtil.normalize(name));
            }
            List<String> lore = new ArrayList<>();
            if (description != null) {
                lore.addAll(FormatUtil.wordWrap(FormatUtil.normalize(description), 30, 30));
            }
            meta.setLore(lore);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    public static @NotNull List<ItemStack> clone(@NotNull List<ItemStack> items) {
        return items.stream().map(ItemStack::clone).toList();
    }

    public static Builder builder(ItemStack stack) {
        return new Builder(stack);
    }

    public static String asString(ItemStack item) {
        var itemType = item.getType().getKey().toString();
        var itemMeta = item.getItemMeta();
        if (itemMeta != null) {
            var componentString = itemMeta.getAsComponentString();
            if (!componentString.isEmpty() && !componentString.equals("[]")) {
                itemType += componentString;
            }
        }
        return itemType + ":" + item.getAmount();
    }

    public static String asShortString(List<ItemStack> items) {
        List<String> shorts = new ArrayList<>();
        for (ItemStack item : items) {
            shorts.add(asShortString(item));
        }
        return "[" + FormatUtil.join(shorts, ", ") + "]";
    }

    public static String asShortString(ItemStack item) {
        if (item == null) {
            return "";
        }
        return item.getAmount() > 1
            ? tr("\u00a7f{0}x \u00a77{1}", item.getAmount(), getItemName(item))
            : tr("\u00a77{0}", getItemName(item));
    }

    @NotNull
    public static ItemStack asDisplayItem(@NotNull ItemStack item) {
        ItemStack copy = new ItemStack(item);
        ItemMeta itemMeta = copy.getItemMeta();
        if (itemMeta != null) {
            itemMeta.addItemFlags(ItemFlag.values());
        }
        copy.setItemMeta(itemMeta);
        return copy;
    }

    @Contract("null -> null; !null -> !null")
    @Nullable
    public static String getItemName(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        var itemMeta = stack.getItemMeta();
        if (itemMeta != null && itemMeta.hasDisplayName() && !itemMeta.getDisplayName().trim().isEmpty()) {
            return stack.getItemMeta().getDisplayName();
        }
        return tr(FormatUtil.camelcase(stack.getType().name()).replaceAll("([A-Z])", " $1").trim());
    }

    /**
     * Builder for ItemStack
     */
    public static class Builder {
        private final ItemStack itemStack;

        public Builder(ItemStack itemStack) {
            this.itemStack = itemStack != null ? itemStack.clone() : new ItemStack(Material.AIR);
        }

        public Builder type(Material mat) {
            itemStack.setType(mat);
            return this;
        }

        public Builder amount(int amount) {
            itemStack.setAmount(amount);
            return this;
        }

        public Builder displayName(String name) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.setDisplayName(name);
            itemStack.setItemMeta(itemMeta);
            return this;
        }

        public Builder enchant(Enchantment enchantment, int level) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.addEnchant(enchantment, level, false);
            itemStack.setItemMeta(itemMeta);
            return this;
        }

        public Builder select(boolean b) {
            return b ? select() : deselect();
        }

        public Builder select() {
            return enchant(Enchantment.PROTECTION, 1).add(ItemFlag.HIDE_ENCHANTS);
        }

        public Builder deselect() {
            return remove(Enchantment.PROTECTION).remove(ItemFlag.HIDE_ENCHANTS);
        }

        public Builder add(ItemFlag... flags) {
            ItemMeta meta = itemStack.getItemMeta();
            meta.addItemFlags(flags);
            itemStack.setItemMeta(meta);
            return this;
        }

        public Builder remove(ItemFlag... flags) {
            ItemMeta meta = itemStack.getItemMeta();
            meta.removeItemFlags(flags);
            itemStack.setItemMeta(meta);
            return this;
        }

        private Builder remove(Enchantment enchantment) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.removeEnchant(enchantment);
            itemStack.setItemMeta(itemMeta);
            return this;
        }

        public Builder lore(String lore) {
            return lore(Collections.singletonList(FormatUtil.normalize(lore)));
        }

        public Builder lore(List<String> lore) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta != null) {
                if (itemMeta.getLore() == null) {
                    itemMeta.setLore(lore);
                } else {
                    List<String> oldLore = itemMeta.getLore();
                    oldLore.addAll(lore);
                    itemMeta.setLore(oldLore);
                }
                itemStack.setItemMeta(itemMeta);
            }
            return this;
        }

        public ItemStack build() {
            return itemStack;
        }
    }

    public record ItemProbability(double probability, ItemStack item) {
    }
}
