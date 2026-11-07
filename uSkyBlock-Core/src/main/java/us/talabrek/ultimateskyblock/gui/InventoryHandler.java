package us.talabrek.ultimateskyblock.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.jetbrains.annotations.NotNull;

public interface InventoryHandler {

    void onClick(@NotNull InventoryClickEvent event);

    /**
     * Whether a quick second press on the clicked slot acts again instead of being ignored as part of a double click.
     */
    default boolean isRepeatable(@NotNull InventoryClickEvent event) {
        return false;
    }

    void onOpen(@NotNull InventoryOpenEvent event);

    void onClose(@NotNull InventoryCloseEvent event);

}
