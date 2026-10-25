package us.talabrek.ultimateskyblock.bootstrap;

import dk.lockfuglsang.minecraft.animation.AnimationHandler;
import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import us.talabrek.ultimateskyblock.MetricsManager;
import us.talabrek.ultimateskyblock.SkyUpdateChecker;
import us.talabrek.ultimateskyblock.api.event.EventLogic;
import us.talabrek.ultimateskyblock.challenge.ChallengeLogic;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigFactory;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.handler.SchematicHandler;
import us.talabrek.ultimateskyblock.hook.HookManager;
import us.talabrek.ultimateskyblock.island.IslandLogic;
import us.talabrek.ultimateskyblock.island.level.AutoIslandLevelRefresh;
import us.talabrek.ultimateskyblock.placeholder.PlaceholderIntegrations;
import us.talabrek.ultimateskyblock.player.PlayerLogic;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.upgrade.IslandRegionRepair;
import us.talabrek.ultimateskyblock.util.Scheduler;
import us.talabrek.ultimateskyblock.uuid.PlayerDB;

import java.time.Duration;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ServicesTest {

    private static final Duration CHECK_EVERY = Duration.ofHours(4);

    @Test
    public void reloadSchedulesTheUpdateChecksAgainOnce() {
        // A /usb reload cancels all of the plugin's tasks, the update checks among them, and starts new services.
        Scheduler scheduler = mock(Scheduler.class);
        uSkyBlock plugin = mock(uSkyBlock.class);

        Services services = services(scheduler);
        services.startup(plugin);
        verify(scheduler, times(1)).async(any(Runnable.class), eq(Duration.ZERO), eq(CHECK_EVERY));

        services.shutdown(plugin);
        services(scheduler).startup(plugin);
        verify(scheduler, times(2)).async(any(Runnable.class), eq(Duration.ZERO), eq(CHECK_EVERY));
    }

    @Test
    public void scheduledCheckSendsNoRequestWhenDisabled() throws Exception {
        RuntimeConfig checkOff = configWithUpdateCheck(false);
        RuntimeConfig checkOn = configWithUpdateCheck(true);
        Scheduler scheduler = mock(Scheduler.class);
        SkyUpdateChecker updateChecker = mock(SkyUpdateChecker.class);
        RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
        uSkyBlock plugin = pluginWith(runtimeConfigs, updateChecker);
        services(scheduler).startup(plugin);
        ArgumentCaptor<Runnable> check = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).async(check.capture(), eq(Duration.ZERO), eq(CHECK_EVERY));

        when(runtimeConfigs.current()).thenReturn(checkOff);
        check.getValue().run();
        verify(updateChecker, never()).checkForUpdates();

        when(runtimeConfigs.current()).thenReturn(checkOn);
        check.getValue().run();
        verify(updateChecker).checkForUpdates();
    }

    private static RuntimeConfig configWithUpdateCheck(boolean check) throws Exception {
        BukkitServerMock.setupServerMock();
        YamlConfiguration config = new YamlConfiguration();
        config.setDefaults(PluginConfigLoader.loadBundledConfig());
        config.set("plugin-updates.check", check);
        return new RuntimeConfigFactory(new GameObjectFactory(), Logger.getAnonymousLogger()).load(config);
    }

    private static uSkyBlock pluginWith(RuntimeConfigs runtimeConfigs, SkyUpdateChecker updateChecker) {
        uSkyBlock plugin = mock(uSkyBlock.class);
        when(plugin.getRuntimeConfigs()).thenReturn(runtimeConfigs);
        when(plugin.getUpdateChecker()).thenReturn(updateChecker);
        doCallRealMethod().when(plugin).checkForUpdatesIfEnabled();
        return plugin;
    }

    private static Services services(Scheduler scheduler) {
        return new Services(mock(AnimationHandler.class), mock(ChallengeLogic.class), mock(EventLogic.class),
            mock(PlayerLogic.class), mock(IslandLogic.class), mock(PlayerDB.class), mock(MetricsManager.class),
            mock(HookManager.class), mock(AutoIslandLevelRefresh.class), mock(PlaceholderIntegrations.class),
            mock(SchematicHandler.class), mock(IslandRegionRepair.class), scheduler);
    }
}
