package us.talabrek.ultimateskyblock.util;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;

import java.time.Duration;
import java.time.Instant;

import static us.talabrek.ultimateskyblock.message.Msg.MUTED;
import static us.talabrek.ultimateskyblock.message.Msg.sendTr;
import static us.talabrek.ultimateskyblock.message.Placeholder.number;

/**
 * General progress tracker using throttling
 */
public class ProgressTracker {
    private final double progressEveryFraction;
    private final Duration progressEvery;
    private final String format;
    private final CommandSender sender;

    private Instant lastProgressTime;
    private double lastProgressFraction;

    public ProgressTracker(CommandSender sender, String format, double progressEveryFraction, Duration progressEvery) {
        this.progressEveryFraction = progressEveryFraction;
        this.progressEvery = progressEvery;
        this.format = format;
        this.sender = sender;
        this.lastProgressTime = Instant.MIN;
    }

    public void progressUpdate(long progress, long total, TagResolver... resolvers) {
        Instant now = Instant.now();
        double progressFraction = (double) progress / (total > 0 ? total : 1d);
        if (now.isAfter(lastProgressTime.plus(progressEvery)) || progressFraction > (lastProgressFraction + progressEveryFraction)) {
            lastProgressFraction = progressFraction;
            lastProgressTime = now;
            int extra = resolvers != null ? resolvers.length : 0;
            TagResolver[] progressResolvers = new TagResolver[extra + 3];
            progressResolvers[0] = number("progress_pct", progressFraction);
            progressResolvers[1] = number("progress", progress);
            progressResolvers[2] = number("total", total);
            if (extra > 0) {
                System.arraycopy(resolvers, 0, progressResolvers, 3, extra);
            }
            sendTr(sender, format, MUTED, progressResolvers);
        }
    }
}
