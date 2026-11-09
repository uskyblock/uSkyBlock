package us.talabrek.ultimateskyblock.command.admin.task;

import dk.lockfuglsang.minecraft.util.TimeUtil;
import dk.lockfuglsang.minecraft.util.Timer;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.ProgressTracker;

import java.time.Duration;
import java.util.List;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;

/**
 * Scans for all players on a list of islands.
 */
public class PurgeTask extends BukkitRunnable {
    private final List<String> purgeList;
    private final PurgeRule rule;
    private final uSkyBlock plugin;
    private final RuntimeConfigs runtimeConfigs;
    private final CommandSender sender;
    private final ProgressTracker tracker;
    private final Timer timer;
    private boolean active;
    private int activeAgain;

    public PurgeTask(uSkyBlock plugin, RuntimeConfigs runtimeConfigs, List<String> purgeList, PurgeRule rule, CommandSender sender) {
        this.plugin = plugin;
        this.runtimeConfigs = runtimeConfigs;
        this.sender = sender;
        this.purgeList = purgeList;
        this.rule = rule;
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
            IslandInfo island = plugin.getIslandInfo(islandName);
            // Off the server thread: the server's player list is copy-on-write, which the server reads off it too.
            if (island != null && !rule.matches(island, PurgeRule.online(plugin.getServer()))) {
                activeAgain++;
                plugin.getLogger().info("Not purging island " + islandName + ": it no longer matches the scan.");
            } else {
                plugin.getIslandLogic().purge(islandName);
            }
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
            plugin.getLogger().info("Finished purging marked inactive islands, " + activeAgain
                + " skipped as active again.");
            if (activeAgain > 0) {
                sendErrorTr(sender, "PURGE:<primary> Skipped <count> islands that are active again.",
                    unparsed("count", String.valueOf(activeAgain)));
            }
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
