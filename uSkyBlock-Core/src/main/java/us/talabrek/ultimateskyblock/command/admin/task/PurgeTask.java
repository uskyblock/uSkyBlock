package us.talabrek.ultimateskyblock.command.admin.task;

import dk.lockfuglsang.minecraft.util.TimeUtil;
import dk.lockfuglsang.minecraft.util.Timer;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.ProgressTracker;

import java.time.Duration;
import java.util.List;
import java.util.logging.Level;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.util.LogUtil.log;

/**
 * Scans for all players on a list of islands.
 */
public class PurgeTask extends BukkitRunnable {
    private final List<String> purgeList;
    private final uSkyBlock plugin;
    private final RuntimeConfigs runtimeConfigs;
    private final CommandSender sender;
    private final ProgressTracker tracker;
    private final Timer timer;
    private boolean active;

    public PurgeTask(uSkyBlock plugin, RuntimeConfigs runtimeConfigs, List<String> purgeList, CommandSender sender) {
        this.plugin = plugin;
        this.runtimeConfigs = runtimeConfigs;
        this.sender = sender;
        this.purgeList = purgeList;
        this.timer = Timer.start();
        Duration feedbackEvery = runtimeConfigs.current().advanced().feedbackEvery();
        tracker = new ProgressTracker(sender,
            marktr("- Purging: <progress_pct:'0%'> (<progress>/<total>), elapsed <elapsed>, estimated completion ~<eta>"),
            25,
            feedbackEvery);
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
            tracker.progressUpdate(
                completed,
                total,
                unparsed("elapsed", TimeUtil.durationAsString(elapsed)),
                unparsed("eta", String.valueOf(TimeUtil.durationAsTicks(eta)))
            );
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
                sendErrorTr(sender, "PURGE:<primary> Finished purging abandoned islands.");
            } else {
                sendErrorTr(sender, "PURGE:<primary> Aborted purging abandoned islands.");
            }
        } finally {
            active = false;
        }
    }
}
