package us.talabrek.ultimateskyblock.command.admin.task;

import dk.lockfuglsang.minecraft.file.FileUtil;
import dk.lockfuglsang.minecraft.util.TimeUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.IslandUtil;
import us.talabrek.ultimateskyblock.util.LogUtil;
import us.talabrek.ultimateskyblock.util.ProgressTracker;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static us.talabrek.ultimateskyblock.util.Msg.send;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

import static dk.lockfuglsang.minecraft.po.I18nUtil.trLegacy;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * An incremental (synchroneous) task for protecting all islands.
 */
public class ProtectAllTask extends BukkitRunnable {
    private static final Logger log = Logger.getLogger(ProtectAllTask.class.getName());
    private final CommandSender sender;
    private final uSkyBlock plugin;
    private final ProgressTracker tracker;
    private final Path islandDirectory;

    private volatile boolean active;

    public ProtectAllTask(final uSkyBlock plugin, final CommandSender sender, Path islandDirectory, ProgressTracker tracker) {
        this.plugin = plugin;
        this.tracker = tracker;
        this.sender = sender;
        this.islandDirectory = islandDirectory;
    }

    public boolean isActive() {
        return active;
    }

    public void stop() {
        active = false;
    }

    @Override
    public void run() {
        active = true;
        long failed = 0;
        long success = 0;
        long skipped = 0;
        Instant tStart = Instant.now();
        try {
            String[] list = islandDirectory.toFile().list(IslandUtil.createIslandFilenameFilter());
            long total = list != null ? list.length : 0;
            if (list != null) {
                for (String fileName : list) {
                    if (!active) {
                        break;
                    }
                    String islandName = FileUtil.getBasename(fileName);
                    IslandInfo islandInfo = plugin.getIslandInfo(islandName);
                    try {
                        if (WorldGuardHandler.protectIsland(plugin, sender, islandInfo)) {
                            success++;
                        } else {
                            skipped++;
                        }
                    } catch (Exception e) {
                        log.log(Level.INFO, "Error occurred trying to process " + fileName, e);
                        failed++;
                    }
                    tracker.progressUpdate(
                        success + failed + skipped,
                        total,
                        unparsed("failed", String.valueOf(failed)),
                        unparsed("skipped", String.valueOf(skipped)),
                        unparsed("elapsed", getElapsed(tStart))
                    );
                }
            }
        } finally {
            if (!active) {
                send(sender, tr("<error>Aborted:</error> <muted>Protect-all was aborted."));
            }
            active = false;
        }
        if (sender instanceof Player && ((Player) sender).isOnline()) {
            send(sender, tr("Completed protect-all in <primary><elapsed></primary>. <primary><count></primary> new regions were created.",
                unparsed("elapsed", getElapsed(tStart)),
                unparsed("count", String.valueOf(success))));
        }
        LogUtil.log(Level.INFO, trLegacy("Completed protect-all in <primary><elapsed></primary>. <primary><count></primary> new regions were created.",
            unparsed("elapsed", getElapsed(tStart)),
            unparsed("count", String.valueOf(success))));
    }

    private String getElapsed(Instant tStart) {
        return TimeUtil.durationAsString(Duration.between(tStart, Instant.now()));
    }
}
