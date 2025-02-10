package us.talabrek.ultimateskyblock.event;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.menu.SkyBlockMenu;
import us.talabrek.ultimateskyblock.player.UltimateHolder;

@Singleton
public class MenuEvents implements Listener {
    private final SkyBlockMenu mainMenu;

    @Inject
    public MenuEvents(@NotNull SkyBlockMenu mainMenu) {
        this.mainMenu = mainMenu;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void guiClick(final InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof UltimateHolder holder && holder.getMenuType() == UltimateHolder.MenuType.DEFAULT) {
            mainMenu.onClick(event);
        }
    }
}
