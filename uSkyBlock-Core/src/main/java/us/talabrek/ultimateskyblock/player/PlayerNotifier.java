package us.talabrek.ultimateskyblock.player;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.PluginConfig;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Notifier that tries to minimize spam.
 */
@Singleton
public class PlayerNotifier {
    private final long maxSpam;
    private final LoadingCache<UUID, NotifyMessage> cache = CacheBuilder
        .newBuilder()
        .expireAfterAccess(10, TimeUnit.SECONDS)
        .maximumSize(500)
        .build(
            new CacheLoader<>() {
                @Override
                public @NotNull NotifyMessage load(@NotNull UUID uuid) {
                    return new NotifyMessage(null, 0);
                }
            }
        );

    @Inject
    public PlayerNotifier(@NotNull PluginConfig config) {
        maxSpam = config.getYamlConfig().getInt("general.maxSpam", 3000); // every 3 seconds.
    }

    public synchronized void notifyPlayer(Player player, String message) {
        UUID uuid = player.getUniqueId();
        try {
            NotifyMessage last = cache.get(uuid);
            long now = System.currentTimeMillis();
            if (now >= last.time() + maxSpam || !message.equals(last.message())) {
                cache.put(uuid, new NotifyMessage(message, now));
                player.sendMessage("\u00a7e" + message);
            }
        } catch (ExecutionException e) {
            // Just ignore - we don't care that much
        }
    }

    private record NotifyMessage(String message, long time) {
    }
}
