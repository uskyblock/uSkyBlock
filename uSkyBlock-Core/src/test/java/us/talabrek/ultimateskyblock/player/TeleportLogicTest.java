package us.talabrek.ultimateskyblock.player;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigFactory;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.platform.SpigotTeleports;
import us.talabrek.ultimateskyblock.platform.Teleports;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.Scheduler;
import us.talabrek.ultimateskyblock.world.WorldManager;

import java.io.File;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Spigot's path: the platform's teleports teleport at once, through Bukkit's own teleport. Paper's path, through
 * {@code teleportAsync}, is tested on paper-api in {@code paperTest}.
 */
class TeleportLogicTest {
    private final uSkyBlock plugin = mock(uSkyBlock.class);
    private final WorldManager worldManager = mock(WorldManager.class);
    private final Scheduler scheduler = mock(Scheduler.class);
    private final World world = mock(World.class);
    private final Player player = mock(Player.class);
    private final Logger logger = mock(Logger.class);
    private final Location target = new Location(world, 10.3, 64, -5.7);
    /** As the teleports centre it: on the block, a little above its floor. */
    private final Location centred = new Location(world, 10 + 0.5, 64 + 0.1, -6 + 0.5);

    private TeleportLogic teleportLogic(Teleports teleports, Duration delay) throws Exception {
        BukkitServerMock.setupServerMock();
        I18nUtil.initialize(new File("."), Locale.ENGLISH);
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        when(player.getUniqueId()).thenReturn(new UUID(0, 1));
        YamlConfiguration config = new YamlConfiguration();
        config.setDefaults(PluginConfigLoader.loadBundledConfig());
        config.set("options.island.islandTeleportDelay", delay.toMillis() + "ms");
        RuntimeConfig runtimeConfig = new RuntimeConfigFactory(new GameObjectFactory(), Logger.getAnonymousLogger())
            .load(config);
        RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
        when(runtimeConfigs.current()).thenReturn(runtimeConfig);
        return new TeleportLogic(logger, plugin, worldManager, scheduler, teleports, runtimeConfigs);
    }

    @Test
    void aForcedTeleportIsAtOnceOntoTheMiddleOfTheBlock() throws Exception {
        teleportLogic(new SpigotTeleports(), Duration.ofSeconds(2)).safeTeleport(player, target, true);

        verify(player).teleport(centred, TeleportCause.PLUGIN);
    }

    @Test
    void aDelayedTeleportWaitsForItsTask() throws Exception {
        when(scheduler.sync(any(Runnable.class), eq(Duration.ofSeconds(2)))).thenReturn(mock(BukkitTask.class));
        teleportLogic(new SpigotTeleports(), Duration.ofSeconds(2)).safeTeleport(player, target, false);

        verify(player, never()).teleport(any(Location.class), any(TeleportCause.class));
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).sync(task.capture(), eq(Duration.ofSeconds(2)));
        task.getValue().run();
        verify(player).teleport(centred, TeleportCause.PLUGIN);
    }

    @Test
    void theSpawnTeleportGoesToTheMiddleOfTheSpawnBlock() throws Exception {
        when(worldManager.getWorld()).thenReturn(world);
        when(world.getSpawnLocation()).thenReturn(target);

        teleportLogic(new SpigotTeleports(), Duration.ZERO).spawnTeleport(player, false);

        verify(player).teleport(centred, TeleportCause.PLUGIN);
    }

    @Test
    void everyTeleportGoesThroughThePlatformsTeleports() throws Exception {
        Teleports teleports = mock(Teleports.class);
        when(teleports.teleport(player, centred)).thenReturn(CompletableFuture.completedFuture(true));
        when(worldManager.getWorld()).thenReturn(world);
        when(world.getSpawnLocation()).thenReturn(target);
        TeleportLogic teleportLogic = teleportLogic(teleports, Duration.ZERO);

        teleportLogic.safeTeleport(player, target, true);
        teleportLogic.spawnTeleport(player, true);

        verify(teleports, times(2)).teleport(player, centred);
        verify(player, never()).teleport(any(Location.class), any(TeleportCause.class));
    }

    @Test
    void aTeleportTheServerCannotMakeIsLogged() throws Exception {
        IllegalArgumentException thrown = new IllegalArgumentException("World unloaded");
        doThrow(thrown).when(player).teleport(centred, TeleportCause.PLUGIN);

        teleportLogic(new SpigotTeleports(), Duration.ZERO).safeTeleport(player, target, true);

        verify(logger).log(eq(Level.WARNING), anyString(), eq(thrown));
    }
}
