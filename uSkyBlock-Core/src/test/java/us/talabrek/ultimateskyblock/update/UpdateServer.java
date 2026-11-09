package us.talabrek.ultimateskyblock.update;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.scheduler.BukkitTask;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigFactory;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A server for update tests: the plugin's jar in a plugins folder, the update folder in it, and a server thread
 * that runs what the plugin schedules on it when a test asks.
 */
public final class UpdateServer {

    public static final String JAR_NAME = "uSkyBlock-3.7.1.jar";

    public final Path plugins;
    public final Server server;
    public final uSkyBlock plugin;
    public final Scheduler scheduler;
    private final BlockingQueue<Runnable> serverThread = new LinkedBlockingQueue<>();

    public UpdateServer(Path plugins, String pluginVersion) {
        this.plugins = plugins;
        server = mock(Server.class);
        updateFolder(plugins.resolve("update").toFile());
        plugin = mock(uSkyBlock.class);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getDescription()).thenReturn(new PluginDescriptionFile("uSkyBlock", pluginVersion, "main"));
        when(plugin.getPluginJar()).thenReturn(plugins.resolve(JAR_NAME));
        when(plugin.getDataFolder()).thenReturn(plugins.resolve("uSkyBlock").toFile());
        when(plugin.isEnabled()).thenReturn(true);
        scheduler = mock(Scheduler.class);
        when(scheduler.sync(any(Runnable.class))).thenAnswer(call -> {
            serverThread.add(call.getArgument(0));
            return mock(BukkitTask.class);
        });
        when(scheduler.sync(any(Runnable.class), any(Duration.class))).thenAnswer(call -> {
            serverThread.add(call.getArgument(0));
            return mock(BukkitTask.class);
        });
    }

    /**
     * @return The bundled config with these update settings.
     */
    public static RuntimeConfigs config(boolean check, String branch, boolean autoStage) {
        try {
            BukkitServerMock.setupServerMock();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        YamlConfiguration config = new YamlConfiguration();
        config.setDefaults(PluginConfigLoader.loadBundledConfig());
        config.set("plugin-updates.check", check);
        config.set("plugin-updates.branch", branch);
        config.set("plugin-updates.auto-stage", autoStage);
        RuntimeConfig runtimeConfig = new RuntimeConfigFactory(new GameObjectFactory(), Logger.getAnonymousLogger())
            .load(config);
        RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
        when(runtimeConfigs.current()).thenReturn(runtimeConfig);
        return runtimeConfigs;
    }

    /**
     * Loads the plugin from a remapped copy of its jar outside the plugins folder, as Paper does.
     */
    public Path remapped() {
        Path remapped = plugins.resolve(".paper-remapped").resolve(JAR_NAME);
        when(plugin.getPluginJar()).thenReturn(remapped);
        return remapped;
    }

    public void updateFolder(File folder) {
        when(server.getUpdateFolderFile()).thenReturn(folder);
    }

    public Path staged() {
        return plugins.resolve("update").resolve(JAR_NAME);
    }

    /**
     * Runs the next task scheduled on the server thread, waiting for it to be scheduled.
     */
    public void runNextTask() throws InterruptedException {
        Runnable task = serverThread.poll(30, TimeUnit.SECONDS);
        if (task == null) {
            throw new AssertionError("Nothing was scheduled on the server thread");
        }
        task.run();
    }

    public boolean hasNoTasks() {
        return serverThread.isEmpty();
    }
}
