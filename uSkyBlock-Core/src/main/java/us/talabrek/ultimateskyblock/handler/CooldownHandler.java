package us.talabrek.ultimateskyblock.handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.util.TimeUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Responsible for handling various cooldowns on commands.
 */
@Singleton
public class CooldownHandler {
    // TODO: flatten map to use <UUID, String> as key, use time API instead of long
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final Scheduler scheduler;

    @Inject
    public CooldownHandler(@NotNull Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public int getCooldown(Player player, String cmd) {
        if (player.hasPermission("usb.mod.bypasscooldowns") || player.hasPermission("usb.exempt.cooldown." + cmd)) {
            return 0;
        }
        Map<String, Long> map = cooldowns.get(player.getUniqueId());
        if (map != null) {
            Long timeout = map.get(cmd);
            long now = System.currentTimeMillis();
            return timeout != null && timeout > now ? TimeUtil.millisAsSeconds(timeout - now) : 0;
        }
        return 0;
    }

    public void resetCooldown(final Player player, final String cmd, int cooldownSecs) {
        UUID uuid = player.getUniqueId();
        if (!cooldowns.containsKey(uuid)) {
            Map<String, Long> cdMap = new HashMap<>();
            cooldowns.put(uuid, cdMap);
        }
        if (cooldownSecs == 0) {
            cooldowns.get(uuid).remove(cmd);
            return;
        }
        cooldowns.get(uuid).put(cmd, System.currentTimeMillis() + TimeUtil.secondsAsMillis(cooldownSecs));
        scheduler.sync((Runnable) () -> {
            Map<String, Long> cmdMap = cooldowns.get(player.getUniqueId());
            if (cmdMap != null) {
                cmdMap.remove(cmd);
            }
        }, Duration.ofSeconds(cooldownSecs));
    }

    public boolean clearCooldown(Player player, String cmd) {
        Map<String, Long> cmdMap = cooldowns.get(player.getUniqueId());
        if (cmdMap != null) {
            return cmdMap.remove(cmd) != null;
        }
        return false;
    }

    public Map<String, Long> getCooldowns(UUID uuid) {
        if (cooldowns.containsKey(uuid)) {
            return cooldowns.get(uuid);
        }
        return Collections.emptyMap();
    }
}
