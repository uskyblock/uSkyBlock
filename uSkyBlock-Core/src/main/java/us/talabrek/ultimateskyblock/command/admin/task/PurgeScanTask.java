package us.talabrek.ultimateskyblock.command.admin.task;

import dk.lockfuglsang.minecraft.file.FileUtil;
import dk.lockfuglsang.minecraft.util.Timer;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.IslandUtil;
import us.talabrek.ultimateskyblock.util.ProgressTracker;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;

/**
 * Scans for all players on a list of islands.
 */
// TODO: test this class!!!
public class PurgeScanTask extends BukkitRunnable {
    private final List<String> islandList;
    private final List<String> purgeList;
    private final uSkyBlock plugin;
    private final RuntimeConfigs runtimeConfigs;
    private final CommandSender sender;
    private final Runnable callback;
    private final PurgeRule rule;
    private final ProgressTracker tracker;
    private final Timer timer;
    private final Set<UUID> online;
    private volatile boolean active;
    private boolean done;

    /** On the server thread: it takes the players online then. */
    public PurgeScanTask(uSkyBlock plugin, RuntimeConfigs runtimeConfigs, File islandDir, Duration time, double purgeLevel, CommandSender sender, Runnable callback) {
        this.plugin = plugin;
        this.runtimeConfigs = runtimeConfigs;
        this.sender = sender;
        this.callback = callback;
        this.rule = new PurgeRule(Instant.now().minus(time), purgeLevel, plugin.getPlayerDB());
        int spawnSize = runtimeConfigs.current().general().spawnSize();
        String[] islandList = islandDir.list(IslandUtil.createIslandFilenameFilter(spawnSize));
        this.islandList = new ArrayList<>(Arrays.asList(islandList));
        purgeList = new ArrayList<>();
        Duration feedbackEvery = runtimeConfigs.current().advanced().feedbackEvery();
        timer = Timer.start();
        tracker = new ProgressTracker(sender,
            marktr("- Scanning: <progress_pct:'0%'> (<progress>/<total> failed: <failed>) ~ <elapsed>"),
            25,
            feedbackEvery);
        active = true;
        online = PurgeRule.online(plugin.getServer());
    }

    private void generatePurgeList() {
        int progress = 0;
        int failed = 0;
        int total = islandList.size();
        while (!islandList.isEmpty()) {
            if (!active) {
                break;
            }
            String islandFile = islandList.remove(0);
            String islandName = FileUtil.getBasename(islandFile);
            try {
                IslandInfo islandInfo = plugin.getIslandInfo(islandName);
                if (islandInfo != null && rule.matches(islandInfo, online)) {
                    purgeList.add(islandName);
                }
            } catch (Exception e) {
                failed++;
            }
            progress++;
            tracker.progressUpdate(
                progress,
                total,
                unparsed("failed", String.valueOf(failed)),
                unparsed("elapsed", timer.elapsedAsString())
            );
        }
    }

    public boolean isActive() {
        return active;
    }

    public boolean isDone() {
        return done;
    }

    public void stop() {
        active = false;
    }

    public List<String> getPurgeList() {
        return purgeList;
    }

    public PurgeRule getRule() {
        return rule;
    }

    @Override
    public void run() {
        generatePurgeList();
        if (!active) {
            sendErrorTr(sender, "PURGE:<primary> Scanning aborted.");
            return;
        }
        plugin.getLogger().info("Done scanning - found " + purgeList.size() + " candidates for purging.");
        sendErrorTr(sender, "PURGE:<primary> Scanning done, found <count> candidates below level <level>, ready for purging.",
            unparsed("count", String.valueOf(purgeList.size())),
            unparsed("level", String.valueOf(rule.purgeLevel())));
        done = true;
        if (!purgeList.isEmpty()) {
            callback.run();
        } else {
            active = false;
        }
    }
}
