package us.talabrek.ultimateskyblock.hook.world;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.hook.PluginHook;
import us.talabrek.ultimateskyblock.uSkyBlock;

/**
 * A hook for world management plugins.
 */
public abstract class WorldHook extends PluginHook {

    public WorldHook(@NotNull uSkyBlock plugin, @NotNull String implementing) {
        super(plugin, "World", implementing);
    }

    /**
     * Registers the given {@link World} as the skyblock overworld.
     *
     * @param world World to register.
     */
    public abstract void registerOverworld(@NotNull World world);

    /**
     * Registers the given {@link World} as the skyblock nether world.
     *
     * @param world World to register.
     */
    public abstract void registerNetherworld(@NotNull World world);
}
