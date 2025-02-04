package us.talabrek.ultimateskyblock.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public abstract class InventoryGui implements InventoryHandler {

    private static final Set<ClickType> REGULAR_CLICK_TYPES = Set.of(
        ClickType.LEFT,
        ClickType.RIGHT,
        ClickType.SHIFT_LEFT,
        ClickType.SHIFT_RIGHT,
        ClickType.DOUBLE_CLICK,
        ClickType.CREATIVE
    );
    private final Inventory inventory;
    private final Map<Integer, InventoryButton> buttonMap = new HashMap<>();

    public InventoryGui(@NotNull Inventory inventory) {
        this.inventory = inventory;
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public void addButton(int slot, InventoryButton button) {
        this.buttonMap.put(slot, button);
    }

    public void decorate(Player player) {
        this.buttonMap.forEach((slot, button) -> {
            ItemStack icon = button.getIconCreator().apply(player);
            this.inventory.setItem(slot, icon);
        });
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!Objects.equals(event.getClickedInventory(), this.inventory)) {
            // Ignore clicks in other (lower) inventory our outside the inventory.
            return;
        }
        if (!REGULAR_CLICK_TYPES.contains(event.getClick())) {
            // Ignore non-regular click types.
            return;
        }
        int slot = event.getSlot();
        InventoryButton button = this.buttonMap.get(slot);
        if (button != null) {
            button.getEventConsumer().accept((Player) event.getWhoClicked(), event);
        }
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        this.decorate((Player) event.getPlayer());
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
    }
}
