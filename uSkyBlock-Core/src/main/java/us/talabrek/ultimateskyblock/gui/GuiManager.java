package us.talabrek.ultimateskyblock.gui;

import com.google.inject.Singleton;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/*
 * New GUI system as of Feb 2025.
 * This system is designed to be more flexible, easier to use and extend, and to separate concerns.
 * All old GUIs from us.talabrek.ultimateskyblock.menu should be converted to this system.
 */
@Singleton
public class GuiManager {

    private final Map<Inventory, InventoryHandler> activeInventories = new HashMap<>();

    public void openGui(@NotNull InventoryGui gui, @NotNull Player player) {
        requireNonNull(gui);
        requireNonNull(player);
        this.registerHandledInventory(gui.getInventory(), gui);
        player.openInventory(gui.getInventory());
    }

    public void registerHandledInventory(@NotNull Inventory inventory, @NotNull InventoryHandler handler) {
        requireNonNull(inventory);
        requireNonNull(handler);
        this.activeInventories.put(inventory, handler);
    }

    public void unregisterInventory(@NotNull Inventory inventory) {
        requireNonNull(inventory);
        this.activeInventories.remove(inventory);
    }

    public void handleClick(@NotNull InventoryClickEvent event) {
        requireNonNull(event);
        InventoryHandler handler = this.activeInventories.get(event.getInventory());
        if (handler != null) {
            event.setCancelled(true);
            handler.onClick(event);
        }
    }

    public void handleOpen(@NotNull InventoryOpenEvent event) {
        requireNonNull(event);
        InventoryHandler handler = this.activeInventories.get(event.getInventory());
        if (handler != null) {
            handler.onOpen(event);
        }
    }

    public void handleClose(@NotNull InventoryCloseEvent event) {
        requireNonNull(event);
        Inventory inventory = event.getInventory();
        InventoryHandler handler = this.activeInventories.get(inventory);
        if (handler != null) {
            handler.onClose(event);
            this.unregisterInventory(inventory);
        }
    }
}
