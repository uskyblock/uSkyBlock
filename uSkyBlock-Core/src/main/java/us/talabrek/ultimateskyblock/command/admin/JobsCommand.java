package us.talabrek.ultimateskyblock.command.admin;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.AbstractCommand;
import dk.lockfuglsang.minecraft.command.CompositeCommand;
import dk.lockfuglsang.minecraft.util.TimeUtil;
import org.bukkit.command.CommandSender;
import us.talabrek.ultimateskyblock.async.JobManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

/**
 * Command for reporting and controlling async jobs.
 */
public class JobsCommand extends CompositeCommand {

    @Inject
    public JobsCommand() {
        super("jobs|j", "usb.admin.jobs", marktr("controls async jobs"));

        add(new AbstractCommand("stats|s", "usb.admin.jobs.stats", "show statistics") {
            @Override
            public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
                StringBuilder sb = new StringBuilder();
                sb.append(tr("\u00a79Job Statistics")).append("\n");
                sb.append(tr("\u00a77----------------")).append("\n");
                Map<String, JobManager.Stats> stats = JobManager.getStats();
                List<String> jobs = new ArrayList<>(stats.keySet());
                Collections.sort(jobs);
                sb.append(String.format("\u00a77%-6s %-8s %-8s %-8s %-8s %-8s %-20s\n",
                    tr("#"), tr("ms/job"), tr("ms/tick"), tr("ticks"), tr("act"), tr("time"), tr("name")));
                for (String jobName : jobs) {
                    JobManager.Stats stat = stats.get(jobName);
                    sb.append(String.format("\u00a77%6d %8s %8s %8d \u00a7c%8d \u00a77%8s \u00a79%-20s \n", stat.getJobs(),
                        TimeUtil.durationAsShort(stat.getAvgRunningTimePerJob()),
                        TimeUtil.durationAsShort(stat.getAvgRunningTimePerTick()),
                        stat.getTicks(),
                        stat.getRunningJobs(),
                        TimeUtil.durationAsShort(stat.getAvgTimeElapsedPerJob()),
                        tr(jobName)
                    ));
                }
                sender.sendMessage(sb.toString().split("\n"));
                return true;
            }
        });
    }
}
