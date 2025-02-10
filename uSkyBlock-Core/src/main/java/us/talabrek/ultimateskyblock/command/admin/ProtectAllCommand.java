package us.talabrek.ultimateskyblock.command.admin;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.AbstractCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.command.admin.task.ProtectAllTask;
import us.talabrek.ultimateskyblock.island.IslandLogic;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.ProgressTracker;

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
                    sender.sendMessage(tr("\u00a7cTrying to abort protect-all task."));
                    task.stop();
                    return true;
                }
                sender.sendMessage(tr("\u00a74Sorry!\u00a7e A protect-all is already running. Let it complete first, or use \u00a79usb protectall \u00a7cstop"));
                return true;
            }
        }
        sender.sendMessage(tr("\u00a7eStarting a protect-all task. It will take a while."));
        Duration feedbackFrequency = Duration.ofMillis(plugin.getConfig().getLong("async.long.feedbackEvery", 30000));
        ProgressTracker tracker = new ProgressTracker(sender, "\u00a77- Protect-All {0,number,##}% ({1}/{2}, failed:{3}, skipped:{4}) ~ {5}", 10, feedbackFrequency);
        task = new ProtectAllTask(plugin, sender, islandLogic.getIslandDirectory(), tracker);
        task.runTaskAsynchronously(plugin);
        return true;
    }
}
