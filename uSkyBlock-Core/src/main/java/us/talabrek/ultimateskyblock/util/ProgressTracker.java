package us.talabrek.ultimateskyblock.util;

import org.bukkit.command.CommandSender;

import java.time.Duration;
import java.time.Instant;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

/**
 * General progress tracker using throttling
 */
public class ProgressTracker {
    private final double progressEveryPct;
    private final Duration progressEvery;
    private final String format;
    private final CommandSender sender;

    private Instant lastProgressTime;
    private float lastProgressPct;

    public ProgressTracker(CommandSender sender, String format, double progressEveryPct, Duration progressEvery) {
        this.progressEveryPct = progressEveryPct;
        this.progressEvery = progressEvery;
        this.format = format;
        this.sender = sender;
    }

    public void progressUpdate(long progress, long total, Object... args) {
        Instant now = Instant.now();
        float pct = 100f * progress / (total > 0 ? total : 1f);
        if (now.isAfter(lastProgressTime.plus(progressEvery)) || pct > (lastProgressPct + progressEveryPct)) {
            lastProgressPct = pct;
            lastProgressTime = now;
            Object[] newArgs = new Object[args.length + 3];
            newArgs[0] = pct;
            newArgs[1] = progress;
            newArgs[2] = total;
            System.arraycopy(args, 0, newArgs, 3, args.length);
            sender.sendMessage(tr(format, newArgs));
        }
    }
}
