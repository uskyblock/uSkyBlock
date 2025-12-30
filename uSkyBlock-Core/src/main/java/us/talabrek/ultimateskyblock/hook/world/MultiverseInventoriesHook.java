package us.talabrek.ultimateskyblock.hook.world;

import com.onarandombox.multiverseinventories.MultiverseInventories;
import com.onarandombox.multiverseinventories.WorldGroup;
import com.onarandombox.multiverseinventories.profile.WorldGroupManager;
import com.onarandombox.multiverseinventories.share.Sharables;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.hook.HookFailedException;
import us.talabrek.ultimateskyblock.uSkyBlock;

public class MultiverseInventoriesHook extends InventorySyncHook {

    private final MultiverseInventories mvInventories;

    public MultiverseInventoriesHook(@NotNull uSkyBlock plugin) {
        super(plugin, "Multiverse-Inventories");
        this.mvInventories = setupInventories();
    }

    private MultiverseInventories setupInventories() {
        Plugin mvPlugin = plugin.getServer().getPluginManager().getPlugin("Multiverse-Inventories");
        if (mvPlugin instanceof MultiverseInventories) {
            return (MultiverseInventories) mvPlugin;
        }

        throw new HookFailedException("Failed to hook into Multiverse-Inventories");
    }

    @Override
    public void linkNetherInventory(@NotNull World... worlds) {
        WorldGroupManager groupManager = mvInventories.getGroupManager();
        WorldGroup worldGroup = groupManager.getGroup("skyblock");
        if (worldGroup == null) {
            worldGroup = groupManager.newEmptyGroup("skyblock");
            worldGroup.getShares().addAll(Sharables.ALL_DEFAULT);
        }
        for (World world : worlds) {
            worldGroup.addWorld(world);
        }
        groupManager.updateGroup(worldGroup);
    }
}
