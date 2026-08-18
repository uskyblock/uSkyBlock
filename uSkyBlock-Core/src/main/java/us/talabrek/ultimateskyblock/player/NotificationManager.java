package us.talabrek.ultimateskyblock.player;

import com.google.inject.Inject;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Delivers {@link Component}s to senders, preferring the server's own Adventure support.
 *
 * <p>Paper implements {@link Audience} directly on {@link CommandSender}, so no bridge is needed
 * there. Spigot does not, and still needs adventure-platform.</p>
 *
 * <p>The distinction is not cosmetic. adventure-platform picks a rendering path by probing facets,
 * each guarded by an {@code isSupported()} that swallows Throwable; when none applies it returns
 * {@link Audience#empty()}, which discards every message with no exception and no log line. On
 * Paper 26.x that is what happens: the server supplies Adventure, shadowing the older serializer
 * chain adventure-platform 4.4.1 pulls in transitively, and the facets meet an Adventure they were
 * not built against. The live-server harness shows this precisely - paper-max and paper-canary fail
 * the {@code message-delivery} scenario while spigot-min, spigot-max and paper-min all pass, so the
 * bridge is sound on Spigot (including 26.2) and only unusable on modern Paper.</p>
 *
 * <p>Routing through the native audience on Paper therefore skips the broken layer entirely, while
 * Spigot keeps full fidelity - hover, click and hex - rather than being downgraded to legacy colour
 * codes. The bridge is created lazily so it is never constructed on Paper at all.</p>
 */
public class NotificationManager {
    private final Plugin plugin;
    private volatile BukkitAudiences audiences;

    @Inject
    public NotificationManager(Plugin plugin) {
        this.plugin = plugin;
    }

    private @NotNull BukkitAudiences audiences() {
        BukkitAudiences local = audiences;
        if (local == null) {
            synchronized (this) {
                local = audiences;
                if (local == null) {
                    local = BukkitAudiences.create(plugin);
                    audiences = local;
                }
            }
        }
        return local;
    }

    /**
     * Sends the given {@link Component} as message to the {@link Player}'s ActionBar.
     *
     * @param player    Player to send the given message to
     * @param component Component to send to the given player
     */
    public void sendActionBar(@NotNull Player player, @NotNull Component component) {
        if (player instanceof Audience audience) {
            audience.sendActionBar(component);
        } else {
            audiences().player(player).sendActionBar(component);
        }
    }

    public void sendMessage(@NotNull CommandSender sender, @NotNull Component component) {
        if (sender instanceof Audience audience) {
            audience.sendMessage(component);
        } else {
            audiences().sender(sender).sendMessage(component);
        }
    }

    public void shutdown() {
        BukkitAudiences local;
        synchronized (this) {
            local = audiences;
            audiences = null;
        }
        if (local != null) {
            local.close();
        }
    }
}
