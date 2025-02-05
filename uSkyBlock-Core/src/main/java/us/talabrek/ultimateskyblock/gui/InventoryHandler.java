package us.talabrek.ultimateskyblock.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.jetbrains.annotations.NotNull;

public interface InventoryHandler {

    void onClick(@NotNull InventoryClickEvent event);

    void onOpen(@NotNull InventoryOpenEvent event);

    void onClose(@NotNull InventoryCloseEvent event);

}
