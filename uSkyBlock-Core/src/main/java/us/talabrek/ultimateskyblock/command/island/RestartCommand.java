package us.talabrek.ultimateskyblock.command.island;

import com.google.inject.Inject;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.config.Settings;
import us.talabrek.ultimateskyblock.api.event.RestartIslandEvent;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.time.Duration;
import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.trLegacy;
import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.message.Placeholder.number;

public class RestartCommand extends RequireIslandCommand {

    @Inject
    public RestartCommand(@NotNull uSkyBlock plugin) {
        super(plugin, "restart|reset", "usb.island.restart", "?schematic", marktr("delete your island and start a new one."));
        addFeaturePermission("usb.exempt.cooldown.restart", trLegacy("exempt player from restart-cooldown"));
    }

    @Override
    protected boolean doExecute(String alias, Player player, PlayerInfo pi, IslandInfo island, Map<String, Object> data, String... args) {
        if (island.getPartySize() > 1) {
            if (!island.isLeader(player)) {
                sendErrorTr(player, "Only the owner may restart this island. Leave this island first to start your own (/is leave).");
            } else {
                sendErrorTr(player, "You must remove all players from your island before you can restart it. <muted>Use <cmd>/is kick [player]</cmd>. See your current island group with <cmd>/is party</cmd>.");
            }
            return true;
        }
        Duration cooldown = plugin.getCooldownHandler().getCooldown(player, "restart");
        if (cooldown.isPositive()) {
            // I18N: <seconds> is a localized number tag. Tag arguments use DecimalFormat patterns; keep tag name "seconds".
            sendErrorTr(player, "You can restart your island in <seconds> seconds.",
                number("seconds", cooldown.toSeconds()));
            return true;
        } else {
            if (pi.isIslandGenerating()) {
                sendErrorTr(player, "Your island is in the process of generating, you cannot restart now.");
                return true;
            }
            if (plugin.getConfig().getBoolean("island-schemes-enabled", true)) {
                if (args == null || args.length == 0) {
                    player.openInventory(plugin.getMenu().createRestartGUI(player));
                    return true;
                }
            }
            if (plugin.getConfirmHandler().checkCommand(player, "/is restart")) {
                plugin.getCooldownHandler().resetCooldown(player, "restart", Settings.general_cooldownRestart);
                String cSchem = args != null && args.length > 0 ? args[0] : island.getSchematicName();
                plugin.getServer().getPluginManager().callEvent(new RestartIslandEvent(player, pi.getIslandLocation(), cSchem));
                return true;
            } else {
                sendErrorTr(player, "Warning: <muted>Your entire island and all your belongings will be reset.");
                return true;
            }
        }
    }
}
