package us.talabrek.ultimateskyblock.island.level;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.PluginConfig;
import us.talabrek.ultimateskyblock.island.task.RecalculateRunnable;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.time.Duration;

@Singleton
public class AutoIslandLevelRefresh {

    private final uSkyBlock plugin;
    private final PluginConfig config;
    private final Scheduler scheduler;

    private BukkitTask autoRecalculateTask = null;

    @Inject
    public AutoIslandLevelRefresh(
        @NotNull uSkyBlock plugin,
        @NotNull PluginConfig config,
        @NotNull Scheduler scheduler
    ) {
        this.plugin = plugin;
        this.config = config;
        this.scheduler = scheduler;
    }

    public void startup() {
        int refreshEveryMinute = config.getYamlConfig().getInt("options.island.autoRefreshScore", 0);
        if (refreshEveryMinute > 0) {
            Duration refreshRate = Duration.ofMinutes(refreshEveryMinute);
            autoRecalculateTask = scheduler.sync(new RecalculateRunnable(plugin), refreshRate, refreshRate);
        }
    }

    public void shutdown() {
        if (autoRecalculateTask != null) {
            autoRecalculateTask.cancel();
            autoRecalculateTask = null;
        }
    }
}
