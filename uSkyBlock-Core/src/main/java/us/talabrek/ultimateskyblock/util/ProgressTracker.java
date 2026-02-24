package us.talabrek.ultimateskyblock.util;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;

import java.time.Duration;
import java.time.Instant;

import static net.kyori.adventure.text.minimessage.tag.resolver.Formatter.number;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.util.Msg.MUTED;
import static us.talabrek.ultimateskyblock.util.Msg.sendTr;

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
        this.lastProgressTime = Instant.MIN;
    }

    public void progressUpdate(long progress, long total, TagResolver... resolvers) {
        Instant now = Instant.now();
        float pct = 100f * progress / (total > 0 ? total : 1f);
        if (now.isAfter(lastProgressTime.plus(progressEvery)) || pct > (lastProgressPct + progressEveryPct)) {
            lastProgressPct = pct;
            lastProgressTime = now;
            int extra = resolvers != null ? resolvers.length : 0;
            TagResolver[] progressResolvers = new TagResolver[extra + 3];
            progressResolvers[0] = number("progress_pct", pct);
            progressResolvers[1] = unparsed("progress", String.valueOf(progress));
            progressResolvers[2] = unparsed("total", String.valueOf(total));
            if (extra > 0) {
                System.arraycopy(resolvers, 0, progressResolvers, 3, extra);
            }
            sendTr(sender, format, MUTED, progressResolvers);
        }
    }
}
