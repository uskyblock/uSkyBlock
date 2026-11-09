package us.talabrek.ultimateskyblock;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigFactory;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.event.PlayerEvents;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.island.IslandLogic;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.update.ModrinthClient;
import us.talabrek.ultimateskyblock.update.ServerPlatform;
import us.talabrek.ultimateskyblock.update.UpdateStager;
import us.talabrek.ultimateskyblock.util.Scheduler;
import us.talabrek.ultimateskyblock.world.WorldManager;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static dk.lockfuglsang.minecraft.util.FormatUtil.stripFormatting;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class uSkyBlockTest {

    @Test
    public void testStripFormatting() throws Exception {
        String text = "&eHello \u00a7bBabe &l&kYou wanna dance&r with somebody";

        assertThat(stripFormatting(text), is("Hello Babe You wanna dance with somebody"));
    }

    @Test
    public void restartPlayerIslandDoesNotClearIslandWhenSchemeValidationFails() throws Exception {
        uSkyBlock plugin = mock(uSkyBlock.class, Answers.CALLS_REAL_METHODS);
        Player player = mock(Player.class);
        IslandLogic islandLogic = mock(IslandLogic.class);
        setField(plugin, "islandLogic", islandLogic);
        doReturn(false).when(plugin).validateIslandScheme(player, "broken");

        boolean restarted = plugin.restartPlayerIsland(player, new Location(mock(World.class), 0, 64, 0), "broken");

        assertFalse(restarted);
        verifyNoInteractions(islandLogic);
    }

    @Test
    public void updateCheckSendsNoRequestWhenDisabled() throws Exception {
        RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
        SkyUpdateChecker updateChecker = updateCheckerWithoutNetwork(runtimeConfigs);
        uSkyBlock plugin = pluginWithUpdateChecker(runtimeConfigs, updateChecker);
        loadConfigWithUpdateCheck(runtimeConfigs, false);

        plugin.checkForUpdatesIfEnabled();

        verify(updateChecker, never()).fetchUpdates();
    }

    @Test
    public void updateCheckSendsRequestWhenEnabled() throws Exception {
        RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
        SkyUpdateChecker updateChecker = updateCheckerWithoutNetwork(runtimeConfigs);
        uSkyBlock plugin = pluginWithUpdateChecker(runtimeConfigs, updateChecker);
        loadConfigWithUpdateCheck(runtimeConfigs, true);

        plugin.checkForUpdatesIfEnabled();

        verify(updateChecker).fetchUpdates();
    }

    @Test
    public void updateCheckFollowsReloadedConfig() throws Exception {
        RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
        SkyUpdateChecker updateChecker = updateCheckerWithoutNetwork(runtimeConfigs);
        uSkyBlock plugin = pluginWithUpdateChecker(runtimeConfigs, updateChecker);

        loadConfigWithUpdateCheck(runtimeConfigs, true);
        plugin.checkForUpdatesIfEnabled();
        loadConfigWithUpdateCheck(runtimeConfigs, false);
        plugin.checkForUpdatesIfEnabled();

        verify(updateChecker, times(1)).fetchUpdates();
    }

    @Test
    public void anEntryClearsAMarkedPlayerInTheIslandWorld() throws Exception {
        EntryClear clear = new EntryClear(true, true, true);

        clear.plugin.clearInventoryOnEntry(clear.player);

        verify(clear.inventory).clear();
        verify(clear.playerInfo).setClearInventoryOnNextEntry(false);
    }

    @Test
    public void anEntryLeavesAnUnmarkedPlayer() throws Exception {
        EntryClear clear = new EntryClear(true, true, false);

        clear.plugin.clearInventoryOnEntry(clear.player);

        verify(clear.inventory, never()).clear();
    }

    @Test
    public void anEntryLeavesAMarkedPlayerOutsideTheIslandWorld() throws Exception {
        EntryClear clear = new EntryClear(true, false, true);

        clear.plugin.clearInventoryOnEntry(clear.player);

        verify(clear.inventory, never()).clear();
        verify(clear.playerInfo, never()).setClearInventoryOnNextEntry(false);
    }

    @Test
    public void anEntryLeavesAMarkedPlayerWhoHasQuit() throws Exception {
        EntryClear clear = new EntryClear(false, true, true);

        clear.plugin.clearInventoryOnEntry(clear.player);

        verify(clear.inventory, never()).clear();
        verify(clear.playerInfo, never()).setClearInventoryOnNextEntry(false);
    }

    @Test
    public void theClearOfAPlayerWhoHasQuitWaitsForTheirNextEntry() throws Exception {
        EntryClear clear = new EntryClear(false, true, false);

        clear.plugin.clearPlayerInventory(clear.player);

        verify(clear.inventory, never()).clear();
        verify(clear.playerInfo).setClearInventoryOnNextEntry(true);
        verify(clear.scheduler, never()).sync(any(Runnable.class), any(Duration.class));
    }

    @Test
    public void theClearOfThePlayerFromBeforeARelogClearsTheRejoinedPlayerOnceTheirArrivalHasSettled() throws Exception {
        EntryClear clear = new EntryClear(true, true, false);
        Player rejoined = clear.reconnect();
        PlayerInventory rejoinedInventory = rejoined.getInventory();
        clear.plugin.clearInventoryOnEntry(rejoined);

        clear.plugin.clearPlayerInventory(clear.player);

        verify(clear.inventory, never()).clear();
        verify(rejoinedInventory, never()).clear();
        assertTrue(clear.playerInfo.isClearInventoryOnNextEntry());
        ArgumentCaptor<Runnable> settled = ArgumentCaptor.forClass(Runnable.class);
        verify(clear.scheduler).sync(settled.capture(), eq(PlayerEvents.ENTRY_SETTLE));
        settled.getValue().run();
        verify(rejoinedInventory).clear();
        verify(clear.inventory, never()).clear();
        assertFalse(clear.playerInfo.isClearInventoryOnNextEntry());
    }

    @Test
    public void anEntryLeavesAMarkedPlayerWhoReconnected() throws Exception {
        EntryClear clear = new EntryClear(true, true, true);
        clear.reconnect();

        clear.plugin.clearInventoryOnEntry(clear.player);

        verify(clear.inventory, never()).clear();
        verify(clear.playerInfo, never()).setClearInventoryOnNextEntry(false);
    }

    /**
     * A player, online or quit, in or outside the island world, with or without a clear marked for their entry. The
     * Player is the one the server knows by the UUID while they are online.
     */
    private static final class EntryClear {
        final uSkyBlock plugin = mock(uSkyBlock.class, Answers.CALLS_REAL_METHODS);
        final Server server = mock(Server.class);
        final Scheduler scheduler = mock(Scheduler.class);
        final World world = mock(World.class);
        final PlayerInfo playerInfo = mock(PlayerInfo.class);
        final UUID uuid = UUID.randomUUID();
        final Player player;
        final PlayerInventory inventory;
        private boolean marked;

        EntryClear(boolean online, boolean inIslandWorld, boolean marked) throws Exception {
            this.marked = marked;
            RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
            BukkitServerMock.setupServerMock();
            YamlConfiguration config = new YamlConfiguration();
            config.setDefaults(PluginConfigLoader.loadBundledConfig());
            RuntimeConfig runtimeConfig =
                new RuntimeConfigFactory(new GameObjectFactory(), Logger.getAnonymousLogger()).load(config);
            when(runtimeConfigs.current()).thenReturn(runtimeConfig);
            setField(plugin, "runtimeConfigs", runtimeConfigs);
            doReturn(Logger.getAnonymousLogger()).when(plugin).getLogger();
            WorldManager worldManager = mock(WorldManager.class);
            when(worldManager.isSkyWorld(world)).thenReturn(inIslandWorld);
            doReturn(worldManager).when(plugin).getWorldManager();
            doReturn(server).when(plugin).getServer();
            doReturn(scheduler).when(plugin).getScheduler();
            when(playerInfo.isClearInventoryOnNextEntry()).thenAnswer(call -> this.marked);
            doAnswer(call -> this.marked = call.getArgument(0)).when(playerInfo).setClearInventoryOnNextEntry(anyBoolean());
            player = player(online);
            inventory = player.getInventory();
            when(server.getPlayer(uuid)).thenReturn(online ? player : null);
        }

        /** The player quits and joins again, so the server knows them by a new Player; the old one stays online. */
        Player reconnect() {
            Player rejoined = player(true);
            when(server.getPlayer(uuid)).thenReturn(rejoined);
            return rejoined;
        }

        private Player player(boolean online) {
            Player player = mock(Player.class);
            doReturn(playerInfo).when(plugin).getPlayerInfo(player);
            when(player.getUniqueId()).thenReturn(uuid);
            when(player.isOnline()).thenReturn(online);
            when(player.getWorld()).thenReturn(world);
            PlayerInventory inventory = mock(PlayerInventory.class);
            when(player.getInventory()).thenReturn(inventory);
            EntityEquipment equipment = mock(EntityEquipment.class);
            when(equipment.getArmorContents()).thenReturn(new ItemStack[4]);
            when(player.getEquipment()).thenReturn(equipment);
            when(player.getEnderChest()).thenReturn(mock(Inventory.class));
            return player;
        }
    }

    private static SkyUpdateChecker updateCheckerWithoutNetwork(RuntimeConfigs runtimeConfigs) {
        SkyUpdateChecker updateChecker = spy(
            new SkyUpdateChecker(mock(uSkyBlock.class), runtimeConfigs, Logger.getAnonymousLogger(),
                mock(ModrinthClient.class), mock(UpdateStager.class), ServerPlatform.spigot("26.3")));
        doReturn(CompletableFuture.completedFuture(null)).when(updateChecker).fetchUpdates();
        return updateChecker;
    }

    private static uSkyBlock pluginWithUpdateChecker(RuntimeConfigs runtimeConfigs, SkyUpdateChecker updateChecker)
        throws Exception {
        uSkyBlock plugin = mock(uSkyBlock.class, Answers.CALLS_REAL_METHODS);
        setField(plugin, "runtimeConfigs", runtimeConfigs);
        setField(plugin, "updateChecker", updateChecker);
        return plugin;
    }

    private static void loadConfigWithUpdateCheck(RuntimeConfigs runtimeConfigs, boolean check) throws Exception {
        BukkitServerMock.setupServerMock();
        YamlConfiguration config = new YamlConfiguration();
        config.setDefaults(PluginConfigLoader.loadBundledConfig());
        config.set("plugin-updates.check", check);
        RuntimeConfig runtimeConfig =
            new RuntimeConfigFactory(new GameObjectFactory(), Logger.getAnonymousLogger()).load(config);
        when(runtimeConfigs.current()).thenReturn(runtimeConfig);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        var field = uSkyBlock.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
