package us.talabrek.ultimateskyblock.hook.world;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.mvplugins.multiverse.core.MultiverseCore;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.MultiverseWorld;
import org.mvplugins.multiverse.core.world.WorldManager;
import org.mvplugins.multiverse.inventories.MultiverseInventories;
import org.mvplugins.multiverse.inventories.MultiverseInventoriesApi;
import org.mvplugins.multiverse.inventories.profile.group.WorldGroup;
import org.mvplugins.multiverse.inventories.profile.group.WorldGroupManager;
import org.mvplugins.multiverse.inventories.share.Sharables;
import us.talabrek.ultimateskyblock.Settings;
import us.talabrek.ultimateskyblock.hook.PluginHook;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.LocationUtil;

import java.util.Optional;

public class MultiverseHook extends PluginHook {
    private MultiverseCore mvCore;
    private MultiverseInventories mvInventories;

    private static final String GENERATOR_NAME = "uSkyBlock";

    public MultiverseHook(@NotNull uSkyBlock plugin) {
        super(plugin, "Multiverse", "Multiverse");

        if (plugin.getServer().getPluginManager().isPluginEnabled("Multiverse-Core")) {
            setupCore().ifPresent(mvPlugin -> this.mvCore = mvPlugin);
        }
        if (plugin.getServer().getPluginManager().isPluginEnabled("Multiverse-Inventories")) {
            setupInventories().ifPresent(mvPlugin -> this.mvInventories = mvPlugin);
        }
    }

    private Optional<MultiverseCore> setupCore() {
        Plugin mvPlugin = plugin.getServer().getPluginManager().getPlugin("Multiverse-Core");
        if (mvPlugin instanceof MultiverseCore) {
            plugin.getLogger().info("Found Multiverse-Core.");
            return Optional.of((MultiverseCore) mvPlugin);
        }

        return Optional.empty();
    }

    private Optional<MultiverseInventories> setupInventories() {
        Plugin mvPlugin = plugin.getServer().getPluginManager().getPlugin("Multiverse-Inventories");
        if (mvPlugin instanceof MultiverseInventories) {
            plugin.getLogger().info("Found Multiverse-Inventories.");
            return Optional.of((MultiverseInventories) mvPlugin);
        }

        return Optional.empty();
    }

    /**
     * Registers the given {@link World} to {@link MultiverseCore} as the skyblock overworld (skyworld).
     * @param world World to register.
     */
    public void registerOverworld(@NotNull World world) {
        if (mvCore == null) {
            return;
        }

        WorldManager mvWorldManager = MultiverseCoreApi.get().getWorldManager();

        if (!mvWorldManager.isWorld(world.getName())) {
            //mvWorldManager.addWorld(world.getName(), World.Environment.NORMAL,
            //    "0", WorldType.NORMAL, false, GENERATOR_NAME, false);
            // The new MV API doesn't seem to provide methods to import a World when it is already loaded by the server.
            plugin.getLogger().severe("the Skyblock overworld is loaded by the server but not found throught the Multiverse-core API");
        }

        MultiverseWorld mvWorld = mvWorldManager.getWorld(world).get();
        //mvWorld.setEnvironment(World.Environment.NORMAL);
        mvWorld.setScale(1.0);
        //mvWorld.setGenerator(GENERATOR_NAME);

        if (Settings.general_spawnSize > 0 && LocationUtil.isEmptyLocation(mvWorld.getSpawnLocation())) {
            Location spawn = LocationUtil.centerOnBlock(
                new Location(world, 0.5, Settings.island_height + 0.1, 0.5));
            mvWorld.setAdjustSpawn(false);
            mvWorld.setSpawnLocation(spawn);
            world.setSpawnLocation(spawn);
        }

        if (!Settings.extras_sendToSpawn) {
            mvWorld.setRespawnWorld(mvWorld);
        }
    }

    /**
     * Registers the given {@link World} to {@link MultiverseCore} as the skyblock nether world (skyworld_nether).
     * @param world World to register.
     */
    public void registerNetherworld(@NotNull World world) {
        if (mvCore == null) {
            return;
        }

        WorldManager mvWorldManager = MultiverseCoreApi.get().getWorldManager();

        if (!mvWorldManager.isWorld(world.getName())) {
            //mvWorldManager.addWorld(world.getName(), World.Environment.NETHER,
            //    "0", WorldType.NORMAL, false, GENERATOR_NAME, false);
            // The new MV API doesn't seem to provide methods to import a World when it is already loaded by the server.
            plugin.getLogger().severe("the Skyblock nether is loaded by the server but not found throught the Multiverse-core API");
        }

        MultiverseWorld mvWorld = mvWorldManager.getWorld(world).get();
        //mvWorld.setEnvironment(World.Environment.NETHER);
        mvWorld.setScale(1.0);
        //mvWorld.setGenerator(GENERATOR_NAME);
        if (Settings.general_spawnSize > 0 && LocationUtil.isEmptyLocation(mvWorld.getSpawnLocation())) {
            Location spawn = LocationUtil.centerOnBlock(
                new Location(world, 0.5, Settings.island_height/2.0 + 0.1, 0.5));
            mvWorld.setAdjustSpawn(false);
            mvWorld.setSpawnLocation(spawn);
            world.setSpawnLocation(spawn);
        }

        if (!Settings.extras_sendToSpawn) {
            mvWorld.setRespawnWorld(plugin.getWorldManager().getWorld().getName());
        }

        linkNetherInventory(plugin.getWorldManager().getWorld(), world);
    }

    private void linkNetherInventory(@NotNull World... worlds) {
        if (mvCore == null || mvInventories == null) {
            return;
        }

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
