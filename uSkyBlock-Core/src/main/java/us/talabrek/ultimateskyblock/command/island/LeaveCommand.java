package us.talabrek.ultimateskyblock.command.island;

import com.google.inject.Inject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.message.Msg.sendTr;

public class LeaveCommand extends RequireIslandCommand {

    @Inject
    public LeaveCommand(@NotNull uSkyBlock plugin) {
        super(plugin, "leave", "usb.party.leave", marktr("leave your party"));
    }

    @Override
    protected boolean doExecute(String alias, Player player, PlayerInfo pi, IslandInfo island, Map<String, Object> data, String... args) {
        if (player.getWorld().getName().equalsIgnoreCase(plugin.getWorldManager().getWorld().getName())) {
            if (!island.isParty()) {
                sendErrorTr(player, "You can't leave your island if you are the only person. <muted>Use <cmd>/is restart</cmd> if you want a new one.");
                return true;
            }
            if (island.isLeader(player)) {
                sendTr(player, "You own this island. <muted>Use <cmd>/is remove [player]</cmd> instead.</muted>");
                return true;
            }
            if (plugin.getConfirmHandler().checkCommand(player, "/is leave")) {
                island.removeMember(pi);
                plugin.getTeleportLogic().spawnTeleport(player, true);
                sendTr(player, "You left the island and returned to player spawn.");
                if (Bukkit.getPlayer(island.getLeader()) != null) {
                    sendErrorTr(Bukkit.getPlayer(island.getLeader()), "has left your island!",
                        unparsed("player", player.getName()));
                }
            }
        } else {
            sendErrorTr(player, "You must be in the skyblock world to leave your party!");
        }
        return true;
    }
}
