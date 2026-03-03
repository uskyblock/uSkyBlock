package us.talabrek.ultimateskyblock.biome;

import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.gui.InventoryButton;
import us.talabrek.ultimateskyblock.gui.InventoryGui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.trLegacy;
import static java.util.Objects.requireNonNull;
import static us.talabrek.ultimateskyblock.message.Msg.MUTED;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Msg.SECONDARY;
import static us.talabrek.ultimateskyblock.message.Placeholder.component;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;

public class BiomeGui extends InventoryGui {

    private static final int MAX_INVENTORY_SIZE = 54;
    private static final int MIN_INVENTORY_SIZE = 18;
    private static final int ROW_SIZE = 9;
    private static final List<String> RADIUS_OPTIONS =
        List.of("10", "chunk", "20", "30", "40", "50", "60", "70", "80", "90", "100", "all");

    private final List<BiomeEntry> biomes;
    private final Biome currentBiome;
    private final int inventorySize;
    private String radius = "all";

    public BiomeGui(@NotNull List<BiomeEntry> biomes, @NotNull Biome currentBiome) {
        super(createInventory(computeInventorySize(biomes.size())));
        this.biomes = requireNonNull(biomes);
        this.currentBiome = requireNonNull(currentBiome);
        ;
        this.inventorySize = computeInventorySize(biomes.size());
    }

    private static int computeInventorySize(int biomesSize) {
        int inventorySize = ((int) Math.ceil((double) biomesSize / (double) ROW_SIZE)) * ROW_SIZE;
        inventorySize += ROW_SIZE; // Add one row for radius controls
        inventorySize = Math.min(inventorySize, MAX_INVENTORY_SIZE);
        inventorySize = Math.max(inventorySize, MIN_INVENTORY_SIZE);
        return inventorySize;
    }

    private static Inventory createInventory(int size) {
        return Bukkit.createInventory(null, size, trLegacy("Island Biome", PRIMARY));
    }

    @Override
    public void decorate(@NotNull Player player) {
        decorateBiomes();
        decorateRadiusControls();
        decorateBackButton();
        super.decorate(player);
    }

    private void decorateBiomes() {
        int displayedBiomes = Math.min(biomes.size(), inventorySize - ROW_SIZE);
        for (int i = 0; i < displayedBiomes; i++) {
            BiomeEntry biomeEntry = biomes.get(i);
            this.addButton(i, createBiomeButton(biomeEntry, currentBiome));
        }
    }

    private InventoryButton createBiomeButton(BiomeEntry biomeEntry, Biome currentBiome) {
        ItemStack displayItem = biomeEntry.displayItem().clone();
        ItemStackUtil.setComponentDisplayName(displayItem, tr("Biome: <biome>", PRIMARY, unparsed("biome", biomeEntry.name())));
        List<Component> lore = Arrays.stream(biomeEntry.description().split("\\R"))
            .filter(line -> !line.isBlank())
            .map(line -> Component.text(line, MUTED))
            .collect(Collectors.toCollection(ArrayList::new));
        if (biomeEntry.biome().equals(currentBiome)) {
            lore.add(tr("This is your current biome.", SECONDARY));
            ItemMeta itemMeta = displayItem.getItemMeta();
            itemMeta.addEnchant(Enchantment.LOYALTY, 1, true);
            displayItem.setItemMeta(itemMeta);
        } else {
            lore.add(tr("Click to change to this biome.", PRIMARY));
        }
        ItemStackUtil.setComponentLore(displayItem, lore);

        return new InventoryButton()
            .creator((player) -> displayItem)
            .consumer((player, event) -> {
                player.performCommand("island biome " + biomeEntry.biome().getKey().getKey() + " " + radius);
                player.closeInventory();
            });
    }

    private void decorateRadiusControls() {
        this.addButton(inventorySize - 6, creteMinusButton());
        this.addButton(inventorySize - 5, createRadiusDisplay());
        this.addButton(inventorySize - 4, createPlusButton());
    }

    private InventoryButton creteMinusButton() {
        return new InventoryButton()
            .creator((player) -> {
                ItemStack displayItem = new ItemStack(Material.RED_CARPET);
                ItemStackUtil.setComponentDisplayName(displayItem, Component.text("-", NamedTextColor.RED));
                List<Component> lore = List.of(
                    tr("Decrease radius of biome change"),
                    tr("Current radius: <radius>", component("radius", formatRadius(radius)))
                );
                ItemStackUtil.setComponentLore(displayItem, lore);
                return displayItem;
            })
            .consumer((player, event) -> {
                int ix = RADIUS_OPTIONS.indexOf(radius);
                if (ix > 0) {
                    radius = RADIUS_OPTIONS.get(ix - 1);
                }
                decorate(player);
            });
    }

    private InventoryButton createPlusButton() {
        return new InventoryButton()
            .creator((player) -> {
                ItemStack displayItem = new ItemStack(Material.GREEN_CARPET);
                ItemStackUtil.setComponentDisplayName(displayItem, Component.text("+", NamedTextColor.GREEN));
                List<Component> lore = List.of(
                    tr("Increase radius of biome change"),
                    tr("Current radius: <radius>", component("radius", formatRadius(radius)))
                );
                ItemStackUtil.setComponentLore(displayItem, lore);
                return displayItem;
            })
            .consumer((player, event) -> {
                int ix = RADIUS_OPTIONS.indexOf(radius);
                if (ix < RADIUS_OPTIONS.size() - 1) {
                    radius = RADIUS_OPTIONS.get(ix + 1);
                }
                decorate(player);
            });
    }

    private InventoryButton createRadiusDisplay() {
        return new InventoryButton()
            .creator((player) -> {
                ItemStack displayItem = new ItemStack(Material.GRASS_BLOCK);
                ItemStackUtil.setComponentDisplayName(displayItem, tr("Current radius: <radius>", component("radius", formatRadius(radius))));
                return displayItem;
            });
    }

    private static Component formatRadius(String radius) {
        return switch (radius) {
            // I18N: Used in biome radius display to indicate that a single chunk is affected
            case "chunk" -> tr("chunk", SECONDARY);
            // I18N: Used in biome radius display to indicate that the entire island is affected
            case "all" -> tr("island", SECONDARY);
            default -> Component.text(radius, SECONDARY);
        };
    }

    private void decorateBackButton() {
        InventoryButton backButton = new InventoryButton()
            .creator((player) -> {
                ItemStack displayItem = new ItemStack(Material.OAK_SIGN);
                ItemStackUtil.setComponentDisplayName(displayItem, tr("Back to Main Menu", PRIMARY));
                ItemStackUtil.setComponentLore(displayItem, List.of(tr("Click here to return to the main island screen.", MUTED)));
                return displayItem;
            })
            .consumer((player, event) -> player.performCommand("island"));
        this.addButton(inventorySize - 9, backButton);
    }
}
