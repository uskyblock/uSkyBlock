package us.talabrek.ultimateskyblock.command.admin;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.AbstractCommand;
import dk.lockfuglsang.minecraft.util.TimeUtil;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.command.admin.task.PurgeScanTask;
import us.talabrek.ultimateskyblock.command.admin.task.PurgeTask;
import us.talabrek.ultimateskyblock.island.IslandLogic;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.time.Duration;
import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

/**
 * The purge-command.
 */
public class PurgeCommand extends AbstractCommand {
    private final uSkyBlock plugin;
    private final IslandLogic islandLogic;
    private final Scheduler scheduler;

    private PurgeScanTask scanTask;
    private PurgeTask purgeTask;

    @Inject
    public PurgeCommand(@NotNull uSkyBlock plugin, @NotNull IslandLogic islandLogic, Scheduler scheduler) {
        super("purge", "usb.admin.purge", "time-in-days|stop|confirm ?level ?force", marktr("purges all abandoned islands"));
        this.plugin = plugin;
        this.islandLogic = islandLogic;
        this.scheduler = scheduler;
    }

    @Override
    public boolean execute(final CommandSender sender, String alias, Map<String, Object> data, String... args) {
        if (purgeActive()) {
            tryConfirm(sender, args);
            return true;
        }
        if (args.length == 0 || !args[0].matches("[0-9]+")) {
            sender.sendMessage(tr("\u00a74You must provide the age in days to purge!"));
            return false;
        }
        String days = args[0];
        double purgeLevel = plugin.getConfig().getDouble("options.advanced.purgeLevel", 10);
        if (args.length > 1 && args[1].matches("[0-9]+([.,][0-9]+)?")) {
            try {
                purgeLevel = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(tr("\u00a74The level must be a valid number"));
                return false;
            }
        }
        final boolean force = args[args.length - 1].equalsIgnoreCase("force");

        Duration time = Duration.ofDays(Integer.parseInt(days, 10));
        sender.sendMessage(tr("\u00a7eFinding all islands that have been abandoned for more than {0} days below level {1}", args[0], purgeLevel));
        scanTask = new PurgeScanTask(plugin, islandLogic.getIslandDirectory().toFile(), time, purgeLevel, sender, () -> {
            if (force) {
                doPurge(sender);
            } else {
                Duration timeout = Duration.ofMillis(plugin.getConfig().getLong("options.advanced.purgeTimeout", 600000)); // TODO: this option does not have an entry in plugin.yml
                sender.sendMessage(tr("\u00a74PURGE:\u00a7e Do \u00a79usb purge confirm\u00a7e within {0} to accept.", TimeUtil.durationAsString(timeout)));
                scheduler.async(() -> {
                    if (scanTask.isActive()) {
                        sender.sendMessage("\u00a77purge timed out");
                        scanTask.stop();
                    }
                }, timeout);
            }
        });
        scanTask.runTaskAsynchronously(plugin);
        return true;
    }

    private boolean purgeActive() {
        return scanTask != null && scanTask.isActive() || purgeTask != null && purgeTask.isActive();
    }

    private void tryConfirm(CommandSender sender, String[] args) {
        if (purgeTask != null && purgeTask.isActive()) {
            if (args.length == 1 && args[0].equalsIgnoreCase("stop")) {
                sender.sendMessage(tr("\u00a74Trying to abort purge"));
                purgeTask.stop();
                return;
            }
        }
        if (scanTask != null && scanTask.isActive() && !scanTask.isDone() && args.length == 1 && args[0].equalsIgnoreCase("stop")) {
            sender.sendMessage(tr("\u00a74Trying to abort purge"));
            scanTask.stop();
            return;
        }
        if (scanTask != null && scanTask.isActive() && scanTask.isDone() && args.length == 1 && args[0].equalsIgnoreCase("confirm")) {
            doPurge(sender);
        } else if (scanTask != null && scanTask.isActive() && scanTask.isDone() && args.length == 1 && args[0].equalsIgnoreCase("stop")) {
            scanTask.stop();
            scanTask = null;
            sender.sendMessage(tr("\u00a74Purge aborted!"));
        } else {
            sender.sendMessage(tr("\u00a74A purge is already running.\u00a7e Either \u00a79confirm\u00a7e or \u00a79stop\u00a7e it."));
        }
    }

    private void doPurge(CommandSender sender) {
        sender.sendMessage(tr("\u00a74Starting purge..."));
        purgeTask = new PurgeTask(plugin, scanTask.getPurgeList(), sender);
        purgeTask.runTaskAsynchronously(plugin);
        scanTask.stop(); // Mark as inactive
    }
}
