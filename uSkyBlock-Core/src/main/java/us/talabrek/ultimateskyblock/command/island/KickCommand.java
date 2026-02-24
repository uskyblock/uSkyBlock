package us.talabrek.ultimateskyblock.command.island;

import com.google.inject.Inject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.legacyArg;
import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.util.Msg.sendErrorTr;

public class KickCommand extends RequireIslandCommand {

    @Inject
    public KickCommand(@NotNull uSkyBlock plugin) {
        super(plugin, "kick|remove", "usb.party.kick", "player", marktr("remove a member from your island."));
    }

    @Override
    protected boolean doExecute(String alias, final Player player, PlayerInfo pi, final IslandInfo island, Map<String, Object> data, final String... args) {
        if (args.length == 1) {
            if (island == null || !island.hasPerm(player, "canKickOthers")) {
                sendErrorTr(player, "You do not have permission to kick others from this island!");
                return true;
            }
            String targetPlayerName = args[0];
            if (island.isLeader(targetPlayerName)) {
                sendErrorTr(player, "You can't remove the leader from the island!");
                return true;
            }
            if (player.getName().equalsIgnoreCase(targetPlayerName)) {
                sendErrorTr(player, "Stop kicking yourself!");
                return true;
            }

            Player onlineTargetPlayer = Bukkit.getPlayer(targetPlayerName);

            if (island.getMembers().contains(targetPlayerName)) {
                PlayerInfo targetPlayerInfo = plugin.getPlayerInfo(targetPlayerName);
                boolean isOnIsland = false;
                if (onlineTargetPlayer != null && onlineTargetPlayer.isOnline()) {
                    sendErrorTr(onlineTargetPlayer,"<player> has removed you from their island!",
                        legacyArg("player", player.getDisplayName()));
                    isOnIsland = plugin.playerIsOnIsland(onlineTargetPlayer);
                }
                if (Bukkit.getPlayer(island.getLeader()) != null) {
                    sendErrorTr(Bukkit.getPlayer(island.getLeader()), "<player> has been removed from the island!",
                        unparsed("player", targetPlayerName));
                }
                island.removeMember(targetPlayerInfo);
                if (isOnIsland && onlineTargetPlayer.isOnline()) {
                    plugin.getTeleportLogic().spawnTeleport(onlineTargetPlayer, true);
                }
            } else if (onlineTargetPlayer != null
                    && onlineTargetPlayer.isOnline()
                    && (plugin.locationIsOnIsland(player, onlineTargetPlayer.getLocation())
                    || plugin.locationIsOnNetherIsland(player, onlineTargetPlayer.getLocation()))
                    ) {
                if (hasPermission(onlineTargetPlayer, "usb.exempt.kick")) {
                    sendErrorTr(onlineTargetPlayer, "<player> tried to kick you from their island!",
                        unparsed("player", player.getName()));
                    sendErrorTr(player, "<player> is exempt from being kicked.",
                        unparsed("player", targetPlayerName));
                    return true;
                }
                sendErrorTr(onlineTargetPlayer, "<player> has kicked you from their island!",
                    unparsed("player", player.getName()));
                sendErrorTr(player, "<player> has been kicked from the island!",
                    unparsed("player", targetPlayerName));
                plugin.getTeleportLogic().spawnTeleport(onlineTargetPlayer, true);
            } else {
                sendErrorTr(player, "That player is not part of your island group, and not on your island!");
            }
            return true;
        }
        return false;
    }
}
