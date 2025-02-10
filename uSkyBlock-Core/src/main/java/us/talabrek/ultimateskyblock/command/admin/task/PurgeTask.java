package us.talabrek.ultimateskyblock.command.admin.task;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import dk.lockfuglsang.minecraft.util.TimeUtil;
import dk.lockfuglsang.minecraft.util.Timer;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.ProgressTracker;

import java.time.Duration;
import java.util.List;
import java.util.logging.Level;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static us.talabrek.ultimateskyblock.util.LogUtil.log;

/**
 * Scans for all players on a list of islands.
 */
public class PurgeTask extends BukkitRunnable {
    private final List<String> purgeList;
    private final uSkyBlock plugin;
    private final CommandSender sender;
    private final ProgressTracker tracker;
    private final Timer timer;
    private boolean active;

    public PurgeTask(uSkyBlock plugin, List<String> purgeList, CommandSender sender) {
        this.plugin = plugin;
        this.sender = sender;
        this.purgeList = purgeList;
        this.timer = Timer.start();
        Duration feedbackEvery = Duration.ofMillis(plugin.getConfig().getInt("async.long.feedbackEvery", 30000));
        tracker = new ProgressTracker(sender, marktr("- PURGING: {0,number,##}% ({1}/{2}), elapsed {3}, estimated completion ~{4}"), 25, feedbackEvery);
        active = true;
    }

    private void doPurge() {
        int total = purgeList.size();
        int completed = 0;
        while (!purgeList.isEmpty()) {
            if (!active) {
                break;
            }
            String islandName = purgeList.removeFirst();
            plugin.getIslandLogic().purge(islandName);
            completed++;
            Duration elapsed = timer.elapsed();
            Duration eta = elapsed.dividedBy(completed).multipliedBy(total - completed);
            tracker.progressUpdate(completed, total, TimeUtil.durationAsString(elapsed), TimeUtil.durationAsTicks(eta));
        }
        plugin.getOrphanLogic().save();
    }

    public boolean isActive() {
        return active;
    }

    public synchronized void stop() {
        active = false;
    }

    @Override
    public void run() {
        try {
            doPurge();
            log(Level.INFO, "Finished purging marked inactive islands.");
            if (active) {
                sender.sendMessage(I18nUtil.tr("\u00a74PURGE:\u00a79 Finished purging abandoned islands."));
            } else {
                sender.sendMessage(I18nUtil.tr("\u00a74PURGE:\u00a79 Aborted purging abandoned islands."));
            }
        } finally {
            active = false;
        }
    }
}
