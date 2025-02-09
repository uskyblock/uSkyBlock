package us.talabrek.ultimateskyblock.handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.po.I18nUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.PluginConfig;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Handles confirms.
 */
@Singleton
public class ConfirmHandler {
    private final Map<UUID, ConfirmCommand> confirmMap = new WeakHashMap<>();
    private final Scheduler scheduler;
    private final PluginConfig config;
    private final Duration timeout;

    @Inject
    public ConfirmHandler(
        @NotNull Scheduler scheduler,
        @NotNull PluginConfig config
    ) {
        this.scheduler = scheduler;
        this.config = config;
        this.timeout = Duration.ofSeconds(config.getYamlConfig().getInt("options.advanced.confirmTimeout", 10));
    }

    public long millisLeft(final Player player, final String cmd) {
        UUID uuid = player.getUniqueId();
        if (confirmMap.containsKey(uuid)) {
            ConfirmCommand command = confirmMap.get(uuid);
            if (command != null && command.isValid(cmd, timeout)) {
                return timeout.minus(Duration.between(command.timeStamp, LocalDateTime.now())).toMillis();
            }
        }
        return -1;
    }

    public boolean checkCommand(final Player player, final String cmd) {
        if (!confirmationsActiveFor(cmd)) {
            return true;
        }
        UUID uuid = player.getUniqueId();
        if (confirmMap.containsKey(uuid)) {
            ConfirmCommand confirmCommand = confirmMap.get(uuid);
            if (confirmCommand != null && confirmCommand.isValid(cmd, timeout)) {
                confirmMap.remove(uuid);
                return true;
            }
        }
        confirmMap.put(uuid, new ConfirmCommand(cmd));
        scheduler.async(() -> {
            ConfirmCommand confirmCommand = confirmMap.remove(player.getUniqueId());
            if (confirmCommand != null && player != null && player.isOnline()) {
                player.sendMessage(I18nUtil.tr("\u00a79{0}\u00a77 timed out", cmd));
            }
        }, timeout);
        player.sendMessage(I18nUtil.tr("\u00a7eDoing \u00a79{0}\u00a7e is \u00a7cRISKY\u00a7e. Repeat the command within \u00a7a{1}\u00a7e seconds to accept!", cmd, timeout));
        return false;
    }

    private boolean confirmationsActiveFor(String cmd) {
        return config.getYamlConfig().getBoolean("confirmation." + cmd.replaceAll("[^a-z\\ ]", ""), true);
    }

    private static class ConfirmCommand {
        private final String command;
        private final LocalDateTime timeStamp;

        private ConfirmCommand(String command) {
            this.command = command;
            this.timeStamp = LocalDateTime.now();
        }

        public boolean isValid(String command, Duration timeout) {
            return this.command.equals(command) && timeStamp.plus(timeout).isAfter(LocalDateTime.now());
        }
    }
}
