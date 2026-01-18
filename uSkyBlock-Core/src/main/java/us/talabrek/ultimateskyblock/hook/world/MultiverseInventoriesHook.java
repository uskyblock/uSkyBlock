package us.talabrek.ultimateskyblock.hook.world;

import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.mvplugins.multiverse.inventories.MultiverseInventories;
import org.mvplugins.multiverse.inventories.MultiverseInventoriesApi;
import org.mvplugins.multiverse.inventories.profile.group.WorldGroup;
import org.mvplugins.multiverse.inventories.profile.group.WorldGroupManager;
import org.mvplugins.multiverse.inventories.share.Sharables;
import us.talabrek.ultimateskyblock.hook.HookFailedException;
import us.talabrek.ultimateskyblock.uSkyBlock;

public class MultiverseInventoriesHook extends InventorySyncHook {

    public MultiverseInventoriesHook(@NotNull uSkyBlock plugin) {
        super(plugin, "Multiverse-Inventories");
        setupInventories();
    }

    private void setupInventories() {
        Plugin mvPlugin = plugin.getServer().getPluginManager().getPlugin("Multiverse-Inventories");
        if (!(mvPlugin instanceof MultiverseInventories)) {
            throw new HookFailedException("Failed to hook into Multiverse-Inventories");
        }
    }

    @Override
    public void linkNetherInventory(@NotNull World... worlds) {
        WorldGroupManager groupManager = MultiverseInventoriesApi.get().getWorldGroupManager();
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
