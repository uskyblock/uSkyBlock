package us.talabrek.ultimateskyblock.command.admin.task;

import dk.lockfuglsang.minecraft.file.FileUtil;
import dk.lockfuglsang.minecraft.util.Timer;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.IslandUtil;
import us.talabrek.ultimateskyblock.util.ProgressTracker;
import us.talabrek.ultimateskyblock.uuid.PlayerDB;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.util.LogUtil.log;

/**
 * Scans for all players on a list of islands.
 */
// TODO: test this class!!!
public class PurgeScanTask extends BukkitRunnable {
    private final List<String> islandList;
    private final List<String> purgeList;
    private final Instant cutOff;
    private final uSkyBlock plugin;
    private final CommandSender sender;
    private final Runnable callback;
    private final double purgeLevel;
    private final ProgressTracker tracker;
    private final Timer timer;
    private final PlayerDB playerDB;
    private volatile boolean active;
    private boolean done;

    public PurgeScanTask(uSkyBlock plugin, File islandDir, Duration time, double purgeLevel, CommandSender sender, Runnable callback) {
        this.plugin = plugin;
        this.sender = sender;
        this.callback = callback;
        this.cutOff = Instant.now().minus(time);
        int spawnSize = plugin.getRuntimeConfigs().current().general().spawnSize();
        String[] islandList = islandDir.list(IslandUtil.createIslandFilenameFilter(spawnSize));
        this.islandList = new ArrayList<>(Arrays.asList(islandList));
        purgeList = new ArrayList<>();
        this.purgeLevel = purgeLevel;
        Duration feedbackEvery = plugin.getRuntimeConfigs().current().advanced().feedbackEvery();
        timer = Timer.start();
        tracker = new ProgressTracker(sender,
            marktr("- Scanning: <progress_pct:'0%'> (<progress>/<total> failed: <failed>) ~ <elapsed>"),
            25,
            feedbackEvery);
        active = true;
        playerDB = plugin.getPlayerDB();
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
                if (islandInfo != null) {
                    Set<UUID> members = islandInfo.getMemberUUIDs();
                    if (!islandInfo.ignore() && islandInfo.getLevel() < purgeLevel && abandonedSince(members)) {
                        purgeList.add(islandName);
                    }
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

    private boolean abandonedSince(Set<UUID> members) {
        for (UUID member : members) {
            OfflinePlayer player = playerDB.getOfflinePlayer(member);
            if (player == null || Instant.ofEpochMilli(player.getLastPlayed()).isAfter(cutOff)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void run() {
        generatePurgeList();
        if (!active) {
            sendErrorTr(sender, "PURGE:<primary> Scanning aborted.");
            return;
        }
        log(Level.INFO, "Done scanning - found " + purgeList.size() + " candidates for purging.");
        sendErrorTr(sender, "PURGE:<primary> Scanning done, found <count> candidates below level <level>, ready for purging.",
            unparsed("count", String.valueOf(purgeList.size())),
            unparsed("level", String.valueOf(purgeLevel)));
        done = true;
        if (!purgeList.isEmpty()) {
            callback.run();
        } else {
            active = false;
        }
    }
}
