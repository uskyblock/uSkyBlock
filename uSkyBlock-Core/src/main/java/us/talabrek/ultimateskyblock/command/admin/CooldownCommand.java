package us.talabrek.ultimateskyblock.command.admin;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.AbstractCommand;
import dk.lockfuglsang.minecraft.command.CompositeCommand;
import dk.lockfuglsang.minecraft.command.completion.AbstractTabCompleter;
import dk.lockfuglsang.minecraft.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.Settings;
import us.talabrek.ultimateskyblock.message.Placeholder;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.legacyArg;
import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.miniToLegacy;
import static dk.lockfuglsang.minecraft.po.I18nUtil.trLegacy;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.util.Msg.ERROR;
import static us.talabrek.ultimateskyblock.util.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.util.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.util.Msg.sendLegacy;
import static us.talabrek.ultimateskyblock.util.Msg.sendTr;

/**
 * Manages player cooldowns
 */
public class CooldownCommand extends CompositeCommand {

    @Inject
    public CooldownCommand(@NotNull uSkyBlock plugin) {
        super("cooldown|cd", "usb.admin.cooldown", marktr("controls player cooldowns"));
        add(new AbstractCommand("clear|c", null, "player command", marktr("clears the cooldown on a command (* = all)")) {
            @Override
            public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
                if (args.length < 2) {
                    return false;
                }
                Player player = Bukkit.getPlayer(args[0]);
                if (player == null || !player.isOnline()) {
                    sendErrorTr(sender, "That player is not currently online.");
                    return false;
                }
                if ("restart|biome".contains(args[1])) {
                    if (plugin.getCooldownHandler().clearCooldown(player, args[1])) {
                        sendTr(sender, "Cleared cooldown on <command> for <player>",
                            unparsed("command", args[1]),
                            legacyArg("player", player.getDisplayName()));
                    } else {
                        sendTr(sender, "No active cooldown on <command> for <player> detected!",
                            unparsed("command", args[1]),
                            legacyArg("player", player.getDisplayName()));
                    }
                    return true;
                } else {
                    sendTr(sender, "Invalid command supplied, only restart and biome supported!");
                    return false;
                }
            }
        });
        add(new AbstractCommand("restart|r", null, "player", marktr("restarts the cooldown on the command")) {
            @Override
            public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
                if (args.length < 2) {
                    return false;
                }
                Player player = Bukkit.getPlayer(args[0]);
                if (player == null || !player.isOnline()) {
                    sendErrorTr(sender, "That player is not currently online.");
                    return false;
                }
                if ("restart|biome".contains(args[1])) {
                    Duration cooldown = getCooldown(args[1]);
                    plugin.getCooldownHandler().resetCooldown(player, args[1], cooldown);
                    sendTr(sender, "Reset cooldown on <command> for <player> to <seconds> seconds.",
                        unparsed("command", args[1], PRIMARY),
                        Placeholder.legacy("player", player.getDisplayName(), PRIMARY),
                        unparsed("seconds", String.valueOf(cooldown.toSeconds()), PRIMARY));
                    return true;
                } else {
                    sendTr(sender, "Invalid command supplied, only restart and biome supported!");
                    return false;
                }
            }
        });
        add(new AbstractCommand("list|l", null, "?player", marktr("lists all the active cooldowns")) {
            @Override
            public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
                if (args.length < 1) {
                    return false;
                }
                Player player = Bukkit.getPlayer(args[0]);
                if (player == null || !player.isOnline()) {
                    sendErrorTr(sender, "That player is not currently online.");
                    return false;
                }
                Map<String, Instant> map = plugin.getCooldownHandler().getCooldowns(player.getUniqueId());
                StringBuilder sb = new StringBuilder();
                if (map != null && !map.isEmpty()) {
                    Instant now = Instant.now();
                    sb.append(trLegacy("Command cooldowns")).append("\n");
                    for (var entry : map.entrySet()) {
                        String cmd = entry.getKey();
                        Duration remainingCooldown = Duration.between(now, entry.getValue());
                        sb.append(miniToLegacy("<command> <duration>",
                            unparsed("command", cmd, PRIMARY),
                            unparsed("duration", TimeUtil.durationAsString(remainingCooldown), ERROR))).append("\n");
                    }
                } else {
                    sb.append(trLegacy("No active cooldowns found for <player>.",
                        Placeholder.legacy("player", player.getDisplayName(), PRIMARY)));
                }
                sendLegacy(sender, sb.toString().split("\n"));
                return true;
            }
        });
        addTab("command", new AbstractTabCompleter() {
            @Override
            protected List<String> getTabList(CommandSender commandSender, String term) {
                return Arrays.asList("restart", "biome");
            }
        });
    }

    private Duration getCooldown(String cmd) {
        return switch (cmd) {
            case "restart" -> Settings.general_cooldownRestart;
            case "biome" -> Settings.general_biomeChange;
            default -> Duration.ZERO;
        };
    }
}
