package us.talabrek.ultimateskyblock.menu;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.file.FileUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * A GUI for managing the uSkyBlock config-files
 */
@Singleton
public class ConfigMenu {

    private final List<EditMenu> editMenus;
    private final MainConfigMenu mainMenu;

    @Inject
    public ConfigMenu(
        @NotNull uSkyBlock plugin,
        @NotNull MenuItemFactory factory
    ) {
        FileConfiguration menuConfig = new YamlConfiguration();
        FileUtil.readConfig(menuConfig, getClass().getClassLoader().getResourceAsStream("configmenu.yml"));
        this.editMenus = new ArrayList<>();
        this.mainMenu = new MainConfigMenu(plugin, menuConfig, factory, editMenus);
        this.editMenus.addAll(List.of(
            new IntegerEditMenu(menuConfig, factory, mainMenu),
            new BooleanEditMenu(menuConfig),
            new StringEditMenu(menuConfig, mainMenu),
            mainMenu // mainMenu goes last (catch all)
        ));
    }

    public void showMenu(Player player, String configName, int page) {
        player.openInventory(mainMenu.createEditMenu(configName, null, page));
    }

    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        for (EditMenu editMenu : editMenus) {
            if (editMenu.onClick(event)) {
                break;
            }
        }
    }
}
