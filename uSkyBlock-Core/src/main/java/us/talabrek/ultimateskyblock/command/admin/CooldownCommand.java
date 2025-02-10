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
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

/**
 * Manages player cooldowns
 */
public class CooldownCommand extends CompositeCommand {

    @Inject
    public CooldownCommand(@NotNull uSkyBlock plugin) {
        super("cooldown|cd", "usb.admin.cooldown", marktr("Controls player-cooldowns"));
        add(new AbstractCommand("clear|c", null, "player command", marktr("clears the cooldown on a command (* = all)")) {
            @Override
            public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
                if (args.length < 2) {
                    return false;
                }
                Player player = Bukkit.getPlayer(args[0]);
                if (player == null || !player.isOnline()) {
                    sender.sendMessage(tr("\u00a7eThe player is not currently online"));
                    return false;
                }
                if ("restart|biome".contains(args[1])) {
                    if (plugin.getCooldownHandler().clearCooldown(player, args[1])) {
                        sender.sendMessage(tr("Cleared cooldown on {0} for {1}", args[1], player.getDisplayName()));
                    } else {
                        sender.sendMessage(tr("No active cooldown on {0} for {1} detected!", args[1], player.getDisplayName()));
                    }
                    return true;
                } else {
                    sender.sendMessage(tr("Invalid command supplied, only restart and biome supported!"));
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
                    sender.sendMessage(tr("\u00a7eThe player is not currently online"));
                    return false;
                }
                if ("restart|biome".contains(args[1])) {
                    Duration cooldown = getCooldown(args[1]);
                    plugin.getCooldownHandler().resetCooldown(player, args[1], cooldown);
                    sender.sendMessage(tr("\u00a7eReset cooldown on {0} for {1}\u00a7e to {2} seconds", args[1], player.getDisplayName(), cooldown));
                    return true;
                } else {
                    sender.sendMessage(tr("Invalid command supplied, only restart and biome supported!"));
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
                    sender.sendMessage(tr("\u00a7eThe player is not currently online"));
                    return false;
                }
                Map<String, Instant> map = plugin.getCooldownHandler().getCooldowns(player.getUniqueId());
                StringBuilder sb = new StringBuilder();
                if (map != null && !map.isEmpty()) {
                    Instant now = Instant.now();
                    sb.append(tr("\u00a7eCmd Cooldown")).append("\n");
                    for (var entry : map.entrySet()) {
                        String cmd = entry.getKey();
                        Duration remainingCooldown = Duration.between(now, entry.getValue());
                        sb.append(tr("\u00a7a{0} \u00a7c{1}", cmd, TimeUtil.durationAsString(remainingCooldown))).append("\n");
                    }
                } else {
                    sb.append(tr("\u00a7eNo active cooldowns for \u00a79{0}\u00a7e found.", data.get("playerName")));
                }
                sender.sendMessage(sb.toString().split("\n"));
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
