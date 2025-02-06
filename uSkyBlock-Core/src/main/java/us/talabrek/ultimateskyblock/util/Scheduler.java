package us.talabrek.ultimateskyblock.util;

import com.google.inject.Inject;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

import static dk.lockfuglsang.minecraft.util.TimeUtil.durationAsTicks;

public class Scheduler {

    private final Plugin plugin;

    @Inject
    public Scheduler(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    public BukkitTask async(Runnable runnable) {
        return Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    public BukkitTask async(Runnable runnable, Duration delay) {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, durationAsTicks(delay));
    }

    public BukkitTask async(Runnable runnable, Duration delay, Duration every) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, durationAsTicks(delay), durationAsTicks(every));
    }

    public BukkitTask sync(Runnable runnable) {
        return Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public BukkitTask sync(Runnable runnable, Duration delay) {
        return Bukkit.getScheduler().runTaskLater(plugin, runnable, durationAsTicks(delay));
    }

    public BukkitTask sync(Runnable runnable, Duration delay, Duration every) {
        return Bukkit.getScheduler().runTaskTimer(plugin, runnable, durationAsTicks(delay), durationAsTicks(every));
    }
}
