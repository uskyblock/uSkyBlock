package us.talabrek.ultimateskyblock.command.admin;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.AbstractCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.command.admin.task.ProtectAllTask;
import us.talabrek.ultimateskyblock.island.IslandLogic;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.ProgressTracker;
import static us.talabrek.ultimateskyblock.util.Msg.send;

import java.time.Duration;
import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

/**
 * Protects all islands with WG regions.
 */
public class ProtectAllCommand extends AbstractCommand {
    private final uSkyBlock plugin;
    private final IslandLogic islandLogic;
    private ProtectAllTask task;

    @Inject
    public ProtectAllCommand(@NotNull uSkyBlock plugin, @NotNull IslandLogic islandLogic) {
        super("protectall", "usb.admin.protectall", marktr("protects all islands (time consuming)"));
        this.plugin = plugin;
        this.islandLogic = islandLogic;
    }

    private boolean isProtectAllActive() {
        return task != null && task.isActive();
    }

    @Override
    public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
        synchronized (plugin) {
            if (isProtectAllActive()) {
                if (task != null && task.isActive() && args.length == 1 && args[0].equals("stop")) {
                    send(sender, tr("<error>Trying to abort protect-all task."));
                    task.stop();
                    return true;
                }
                send(sender, tr("<error>A protect-all task is already running.</error> <muted>Let it complete first, or use <cmd>/usb protectall stop</cmd>."));
                return true;
            }
        }
        send(sender, tr("Starting a protect-all task. It may take a while."));
        Duration feedbackFrequency = Duration.ofMillis(plugin.getConfig().getLong("async.long.feedbackEvery", 30000));
        ProgressTracker tracker = new ProgressTracker(sender,
            marktr("<muted>- Protect-All <progress_pct:'##'>% (<progress>/<total>, failed:<failed>, skipped:<skipped>) ~ <elapsed>"),
            10,
            feedbackFrequency);
        task = new ProtectAllTask(plugin, sender, islandLogic.getIslandDirectory(), tracker);
        task.runTaskAsynchronously(plugin);
        return true;
    }
}
