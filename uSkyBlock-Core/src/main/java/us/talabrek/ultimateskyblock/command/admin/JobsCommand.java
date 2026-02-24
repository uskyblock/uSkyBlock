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
import static dk.lockfuglsang.minecraft.po.I18nUtil.parseMini;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.util.Msg.MUTED;
import static us.talabrek.ultimateskyblock.util.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.util.Msg.plainText;
import static us.talabrek.ultimateskyblock.util.Msg.send;
import static us.talabrek.ultimateskyblock.util.Msg.sendTr;

/**
 * Command for reporting and controlling async jobs.
 */
public class JobsCommand extends CompositeCommand {
    // I18N: Header row for /usb jobs stats table. Keep labels short so columns still align in chat.
    private static final String JOBS_HEADER = marktr("jobs   ms/job   ms/tick  ticks    active   time     name");
    // Not translatable: formatting template with placeholders only.
    private static final String JOBS_ROW = "<jobs> <ms-per-job> <ms-per-tick> <ticks> <active> <elapsed> <name>";

    @Inject
    public JobsCommand() {
        super("jobs|j", "usb.admin.jobs", marktr("controls async jobs"));

        add(new AbstractCommand("stats|s", "usb.admin.jobs.stats", "show statistics") {
            @Override
            public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
                sendTr(sender, "Job Statistics", PRIMARY);
                send(sender, parseMini("<muted>----------------"));
                Map<String, JobManager.Stats> stats = JobManager.getStats();
                List<String> jobs = new ArrayList<>(stats.keySet());
                Collections.sort(jobs);

                sendTr(sender, JOBS_HEADER, MUTED);

                for (String jobName : jobs) {
                    JobManager.Stats stat = stats.get(jobName);
                    send(sender, parseMini(JOBS_ROW,
                        unparsed("jobs", String.format("%6d", stat.getJobs())),
                        unparsed("ms-per-job", String.format("%8s", TimeUtil.durationAsShort(stat.getAvgRunningTimePerJob()))),
                        unparsed("ms-per-tick", String.format("%8s", TimeUtil.durationAsShort(stat.getAvgRunningTimePerTick()))),
                        unparsed("ticks", String.format("%8d", stat.getTicks())),
                        unparsed("active", String.format("%8d", stat.getRunningJobs()), PRIMARY),
                        unparsed("elapsed", String.format("%8s", TimeUtil.durationAsShort(stat.getAvgTimeElapsedPerJob()))),
                        unparsed("name", String.format("%-20s", plainText(tr(jobName))), PRIMARY)
                    ));
                }
                return true;
            }
        });
    }
}
