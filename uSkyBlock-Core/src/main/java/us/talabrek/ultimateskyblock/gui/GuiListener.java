package us.talabrek.ultimateskyblock.gui;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

@Singleton
public class GuiListener implements Listener {
    private final GuiManager guiManager;

    @Inject
    public GuiListener(@NotNull GuiManager guiManager) {
        this.guiManager = requireNonNull(guiManager);
    }

    @EventHandler
    public void onClick(@NotNull InventoryClickEvent event) {
        this.guiManager.handleClick(event);
    }

    @EventHandler
    public void onOpen(@NotNull InventoryOpenEvent event) {
        this.guiManager.handleOpen(event);
    }

    @EventHandler
    public void onClose(@NotNull InventoryCloseEvent event) {
        this.guiManager.handleClose(event);
    }
}
