package us.talabrek.ultimateskyblock.gui;

import dk.lockfuglsang.minecraft.util.ItemStackUtil;
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

import static java.util.Objects.requireNonNull;

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
        this.inventory = requireNonNull(inventory);
    }

    public @NotNull Inventory getInventory() {
        return this.inventory;
    }

    public void addButton(int slot, @NotNull InventoryButton button) {
        requireNonNull(button);
        this.buttonMap.put(slot, button);
    }

    public void decorate(@NotNull Player player) {
        requireNonNull(player);
        this.buttonMap.forEach((slot, button) -> {
            ItemStack icon = button.getIconCreator().apply(player);
            icon = ItemStackUtil.asDisplayItem(icon);
            this.inventory.setItem(slot, icon);
        });
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event) {
        requireNonNull(event);
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
    public void onOpen(@NotNull InventoryOpenEvent event) {
        requireNonNull(event);
        this.decorate((Player) event.getPlayer());
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event) {
    }
}
