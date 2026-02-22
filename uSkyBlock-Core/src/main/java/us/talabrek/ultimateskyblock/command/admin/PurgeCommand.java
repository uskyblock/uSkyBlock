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
import static us.talabrek.ultimateskyblock.util.Msg.send;

import java.time.Duration;
import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

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
            send(sender, tr("<error>You must provide the age in days to purge!"));
            return false;
        }
        String days = args[0];
        double purgeLevel = plugin.getConfig().getDouble("options.advanced.purgeLevel", 10);
        if (args.length > 1 && args[1].matches("[0-9]+([.,][0-9]+)?")) {
            try {
                purgeLevel = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                send(sender, tr("<error>The level must be a valid number"));
                return false;
            }
        }
        final boolean force = args[args.length - 1].equalsIgnoreCase("force");

        Duration time = Duration.ofDays(Integer.parseInt(days, 10));
        send(sender, tr("Finding all islands abandoned for more than <primary><days></primary> days below level <primary><level></primary>.",
            unparsed("days", args[0]),
            unparsed("level", String.valueOf(purgeLevel))));
        scanTask = new PurgeScanTask(plugin, islandLogic.getIslandDirectory().toFile(), time, purgeLevel, sender, () -> {
            if (force) {
                doPurge(sender);
            } else {
                Duration timeout = Duration.ofMillis(plugin.getConfig().getLong("options.advanced.purgeTimeout", 600000)); // TODO: this option does not have an entry in plugin.yml
                send(sender, tr("<error>PURGE:</error> <muted>Run <cmd>/usb purge confirm</cmd> within <primary><timeout></primary> to confirm.",
                    unparsed("timeout", TimeUtil.durationAsString(timeout))));
                scheduler.async(() -> {
                    if (scanTask.isActive()) {
                        send(sender, tr("<error>Purge timed out."));
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
                send(sender, tr("<error>Trying to abort purge"));
                purgeTask.stop();
                return;
            }
        }
        if (scanTask != null && scanTask.isActive() && !scanTask.isDone() && args.length == 1 && args[0].equalsIgnoreCase("stop")) {
            send(sender, tr("<error>Trying to abort purge"));
            scanTask.stop();
            return;
        }
        if (scanTask != null && scanTask.isActive() && scanTask.isDone() && args.length == 1 && args[0].equalsIgnoreCase("confirm")) {
            doPurge(sender);
        } else if (scanTask != null && scanTask.isActive() && scanTask.isDone() && args.length == 1 && args[0].equalsIgnoreCase("stop")) {
            scanTask.stop();
            scanTask = null;
            send(sender, tr("<error>Purge aborted!"));
        } else {
            send(sender, tr("<error>A purge is already running.</error> <muted>Either <primary>confirm</primary> or <primary>stop</primary> it."));
        }
    }

    private void doPurge(CommandSender sender) {
        send(sender, tr("<error>Starting purge..."));
        purgeTask = new PurgeTask(plugin, scanTask.getPurgeList(), sender);
        purgeTask.runTaskAsynchronously(plugin);
        scanTask.stop(); // Mark as inactive
    }
}
