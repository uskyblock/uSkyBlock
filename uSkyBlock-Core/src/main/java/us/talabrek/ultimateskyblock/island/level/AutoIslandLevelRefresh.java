package us.talabrek.ultimateskyblock.island.level;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.PluginConfig;
import us.talabrek.ultimateskyblock.island.task.RecalculateRunnable;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.time.Duration;

import static dk.lockfuglsang.minecraft.util.TimeUtil.durationAsTicks;

@Singleton
public class AutoIslandLevelRefresh {

    private final uSkyBlock plugin;
    private final PluginConfig config;

    private BukkitTask autoRecalculateTask = null;

    @Inject
    public AutoIslandLevelRefresh(@NotNull uSkyBlock plugin, @NotNull PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void startup() {
        int refreshEveryMinute = config.getYamlConfig().getInt("options.island.autoRefreshScore", 0);
        if (refreshEveryMinute > 0) {
            long refreshTicks = durationAsTicks(Duration.ofMinutes(refreshEveryMinute));
            autoRecalculateTask = new RecalculateRunnable(plugin).runTaskTimer(plugin, refreshTicks, refreshTicks);
        }
    }

    public void shutdown() {
        if (autoRecalculateTask != null) {
            autoRecalculateTask.cancel();
            autoRecalculateTask = null;
        }
    }
}
