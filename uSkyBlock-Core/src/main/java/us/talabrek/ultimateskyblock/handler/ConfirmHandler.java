package us.talabrek.ultimateskyblock.handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.PluginConfig;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import static us.talabrek.ultimateskyblock.message.Msg.CMD;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Msg.sendTr;
import static us.talabrek.ultimateskyblock.message.Placeholder.number;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;

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

    public @NotNull Duration durationLeft(@NotNull Player player, @NotNull String cmd) {
        UUID uuid = player.getUniqueId();
        if (confirmMap.containsKey(uuid)) {
            ConfirmCommand command = confirmMap.get(uuid);
            if (command != null && command.isValid(cmd, timeout)) {
                return timeout.minus(Duration.between(command.timeStamp, Instant.now()));
            }
        }
        return Duration.ZERO;
    }

    public boolean checkCommand(@NotNull Player player, @NotNull String command) {
        if (!confirmationsActiveFor(command)) {
            return true;
        }
        UUID uuid = player.getUniqueId();
        if (confirmMap.containsKey(uuid)) {
            ConfirmCommand confirmCommand = confirmMap.get(uuid);
            if (confirmCommand != null && confirmCommand.isValid(command, timeout)) {
                confirmMap.remove(uuid);
                return true;
            }
        }
        confirmMap.put(uuid, new ConfirmCommand(command));
        scheduler.async(() -> {
            ConfirmCommand confirmCommand = confirmMap.remove(uuid);
            if (confirmCommand != null && player.isOnline()) {
                sendTr(player, "<command> timed out.", unparsed("command", command, CMD));
            }
        }, timeout);
        // I18N: <timeout> is a localized number tag. Tag arguments use DecimalFormat patterns; keep tag name "timeout".
        sendTr(player, "Running <command> is <error>risky</error>. <muted>Repeat it within <timeout> seconds to confirm.</muted>",
            unparsed("command", command, CMD),
            number("timeout", timeout.toSeconds(), PRIMARY));
        return false;
    }

    private boolean confirmationsActiveFor(@NotNull String command) {
        return config.getYamlConfig().getBoolean("confirmation." + command.replaceAll("[^a-z\\ ]", ""), true);
    }

    private static class ConfirmCommand {
        private final String command;
        private final Instant timeStamp;

        private ConfirmCommand(String command) {
            this.command = command;
            this.timeStamp = Instant.now();
        }

        public boolean isValid(String command, Duration timeout) {
            return this.command.equals(command) && timeStamp.plus(timeout).isAfter(Instant.now());
        }
    }
}
