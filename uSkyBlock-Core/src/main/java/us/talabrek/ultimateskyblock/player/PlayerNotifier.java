package us.talabrek.ultimateskyblock.player;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.config.PluginConfig;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static us.talabrek.ultimateskyblock.message.Msg.plainText;
import static us.talabrek.ultimateskyblock.message.Msg.send;

/**
 * Notifier that tries to minimize spam.
 */
@Singleton
public class PlayerNotifier {
    private final Duration spawnThreshold;
    private final LoadingCache<UUID, NotifyMessage> cache = CacheBuilder
        .newBuilder()
        .expireAfterAccess(10, TimeUnit.SECONDS)
        .maximumSize(500)
        .build(
            new CacheLoader<>() {
                @Override
                public @NotNull NotifyMessage load(@NotNull UUID uuid) {
                    return new NotifyMessage(null, Instant.MIN);
                }
            }
        );

    @Inject
    public PlayerNotifier(@NotNull PluginConfig config) {
        spawnThreshold = Duration.ofMillis(config.getYamlConfig().getInt("general.maxSpam", 3000)); // every 3 seconds.
    }

    public synchronized void notifyPlayer(Player player, Component message) {
        UUID uuid = player.getUniqueId();
        try {
            NotifyMessage last = cache.get(uuid);
            Instant now = Instant.now();
            String cacheMessage = plainText(message);
            if (now.isAfter(last.time().plus(spawnThreshold)) || !cacheMessage.equals(last.message())) {
                cache.put(uuid, new NotifyMessage(cacheMessage, now));
                send(player, message);
            }
        } catch (ExecutionException e) {
            // Just ignore - we don't care that much
        }
    }

    private record NotifyMessage(String message, Instant time) {
    }
}
