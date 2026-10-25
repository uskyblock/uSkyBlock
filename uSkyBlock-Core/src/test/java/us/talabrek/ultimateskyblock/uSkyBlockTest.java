package us.talabrek.ultimateskyblock;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigFactory;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.island.IslandLogic;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static dk.lockfuglsang.minecraft.util.FormatUtil.stripFormatting;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
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

        verify(updateChecker, never()).fetchLatestVersion(any());
    }

    @Test
    public void updateCheckSendsRequestWhenEnabled() throws Exception {
        RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
        SkyUpdateChecker updateChecker = updateCheckerWithoutNetwork(runtimeConfigs);
        uSkyBlock plugin = pluginWithUpdateChecker(runtimeConfigs, updateChecker);
        loadConfigWithUpdateCheck(runtimeConfigs, true);

        plugin.checkForUpdatesIfEnabled();

        verify(updateChecker).fetchLatestVersion(any());
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

        verify(updateChecker, times(1)).fetchLatestVersion(any());
    }

    private static SkyUpdateChecker updateCheckerWithoutNetwork(RuntimeConfigs runtimeConfigs) {
        SkyUpdateChecker updateChecker = spy(
            new SkyUpdateChecker(mock(uSkyBlock.class), runtimeConfigs, Logger.getAnonymousLogger()));
        doReturn(CompletableFuture.completedFuture(null)).when(updateChecker).fetchLatestVersion(any());
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
