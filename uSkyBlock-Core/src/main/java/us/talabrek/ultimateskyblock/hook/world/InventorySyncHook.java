package us.talabrek.ultimateskyblock.hook.world;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.hook.PluginHook;
import us.talabrek.ultimateskyblock.uSkyBlock;

/**
 * A hook for inventory synchronization plugins.
 */
public abstract class InventorySyncHook extends PluginHook {

    public InventorySyncHook(@NotNull uSkyBlock plugin, @NotNull String implementing) {
        super(plugin, "InventorySync", implementing);
    }

    /**
     * Links worlds together for inventory synchronization.
     * @param worlds The worlds to link.
     */
    public abstract void linkNetherInventory(@NotNull World... worlds);
}
