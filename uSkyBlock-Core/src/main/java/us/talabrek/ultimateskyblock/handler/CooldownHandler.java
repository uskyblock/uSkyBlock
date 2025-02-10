package us.talabrek.ultimateskyblock.handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Responsible for handling various cooldowns on commands.
 */
@Singleton
public class CooldownHandler {

    private final Map<KeyPair, Instant> cooldownExpires = new HashMap<>();
    private final Clock clock;
    private final Scheduler scheduler;

    private record KeyPair(@NotNull UUID uuid, @NotNull String cmd) {
    }

    @Inject
    public CooldownHandler(@NotNull Clock clock, @NotNull Scheduler scheduler) {
        this.clock = clock;
        this.scheduler = scheduler;
    }

    public Duration getCooldown(@NotNull Player player, @NotNull String cmd) {
        if (player.hasPermission("usb.mod.bypasscooldowns") || player.hasPermission("usb.exempt.cooldown." + cmd)) {
            return Duration.ZERO;
        }
        var now = clock.instant();
        var end = cooldownExpires.getOrDefault(new KeyPair(player.getUniqueId(), cmd), now);
        var remaining = Duration.between(now, end);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    public void resetCooldown(@NotNull Player player, @NotNull String cmd, @NotNull Duration cooldown) {
        var key = new KeyPair(player.getUniqueId(), cmd);
        if (cooldown.isZero() || cooldown.isNegative()) {
            cooldownExpires.remove(key);
        } else {
            cooldownExpires.put(key, clock.instant().plus(cooldown));
            scheduler.sync(() -> cooldownExpires.remove(key), cooldown.plusSeconds(1));
        }
    }

    public boolean clearCooldown(@NotNull Player player, @NotNull String cmd) {
        var key = new KeyPair(player.getUniqueId(), cmd);
        return cooldownExpires.remove(key) != null;
    }

    public Map<String, Instant> getCooldowns(@NotNull UUID uuid) {
        return cooldownExpires.entrySet().stream()
            .filter(e -> e.getKey().uuid().equals(uuid))
            .collect(Collectors.toMap(e -> e.getKey().cmd(), Map.Entry::getValue));
    }
}
