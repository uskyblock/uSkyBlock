package us.talabrek.ultimateskyblock.biome;

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
import java.util.Objects;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static java.util.Objects.requireNonNull;

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
        this.currentBiome = requireNonNull(currentBiome);;
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
        return Bukkit.createInventory(null, size, "\u00a79" + tr("Island Biome"));
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
        ItemMeta itemMeta = Objects.requireNonNull(displayItem.getItemMeta());
        itemMeta.setDisplayName("\u00a7a" + tr("Biome: {0}", biomeEntry.name()));
        List<String> lore = new ArrayList<>(
            Arrays.stream(biomeEntry.description().split("\\R"))
                .filter(line -> !line.isBlank())
                .map(line -> "\u00a7f" + line)
                .toList()
        );
        if (biomeEntry.biome().equals(currentBiome)) {
            lore.add(tr("\u00a72\u00a7lThis is your current biome."));
            itemMeta.addEnchant(Enchantment.LOYALTY, 1, true);
        } else {
            lore.add(tr("\u00a7e\u00a7lClick to change to this biome."));
        }
        itemMeta.setLore(lore);
        displayItem.setItemMeta(itemMeta);

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
                ItemMeta itemMeta = requireNonNull(requireNonNull(displayItem.getItemMeta()));
                itemMeta.setDisplayName(tr("\u00a7c-"));
                List<String> lore = List.of(
                    tr("Decrease radius of biome change"),
                    tr("Current radius: {0}", formatRadius(radius))
                );
                itemMeta.setLore(lore);
                displayItem.setItemMeta(itemMeta);
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
                ItemMeta itemMeta = requireNonNull(requireNonNull(displayItem.getItemMeta()));
                itemMeta.setDisplayName(tr("\u00a72+"));
                List<String> lore = List.of(
                    tr("Increase radius of biome change"),
                    tr("Current radius: {0}", formatRadius(radius))
                );
                itemMeta.setLore(lore);
                displayItem.setItemMeta(itemMeta);
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
                ItemMeta itemMeta = requireNonNull(requireNonNull(displayItem.getItemMeta()));
                itemMeta.setDisplayName(tr("Current radius: {0}", formatRadius(radius)));
                displayItem.setItemMeta(itemMeta);
                return displayItem;
            });
    }

    private static String formatRadius(String radius) {
        return switch (radius) {
            case "chunk" -> tr("\u00a72chunk");
            case "all" -> tr("\u00a7call");
            default -> tr("\u00a7e{0}", radius);
        };
    }

    private void decorateBackButton() {
        InventoryButton backButton = new InventoryButton()
            .creator((player) -> {
                ItemStack displayItem = new ItemStack(Material.OAK_SIGN);
                ItemMeta itemMeta = requireNonNull(displayItem.getItemMeta());
                itemMeta.setDisplayName(tr("\u00a7cBack to Main Menu"));
                itemMeta.setLore(List.of(tr("\u00a7eClick here to return to the main island screen.")));
                displayItem.setItemMeta(itemMeta);
                return displayItem;
            })
            .consumer((player, event) -> player.performCommand("island"));
        this.addButton(inventorySize - 9, backButton);
    }
}
