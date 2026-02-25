package us.talabrek.ultimateskyblock.command.island;

import com.google.inject.Inject;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.message.Placeholder;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.parseMini;
import static dk.lockfuglsang.minecraft.po.I18nUtil.trLegacy;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.message.Msg.ERROR;
import static us.talabrek.ultimateskyblock.message.Msg.MUTED;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Msg.SUCCESS;
import static us.talabrek.ultimateskyblock.message.Msg.send;
import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.message.Msg.sendTr;

public class BanCommand extends RequireIslandCommand {

    @Inject
    public BanCommand(@NotNull uSkyBlock plugin) {
        super(plugin, "ban|unban", "usb.island.ban", "player", marktr("ban/unban a player from your island."));
        addFeaturePermission("usb.exempt.ban", trLegacy("exempts user from being banned"));
    }

    @Override
    protected boolean doExecute(String alias, Player player, PlayerInfo pi, IslandInfo island, Map<String, Object> data, String... args) {
        if (args.length == 0) {
            sendTr(player, "The following players are banned from warping to your island:");
            send(player, parseMini("<error><bans>", unparsed("bans", String.join(", ", island.getBans()))));
            sendTr(player, "To ban or unban on your island, use <cmd>/is ban [player]</cmd>.", MUTED);
            return true;
        } else if (args.length == 1) {
            String name = args[0];
            if (island.getMembers().contains(name)) {
                sendErrorTr(player, "You can't ban members. Remove them first!");
                return true;
            }
            if (!island.hasPerm(player, "canKickOthers")) {
                sendErrorTr(player, "You do not have permission to kick/ban players.");
                return true;
            }
            if (!island.isBanned(name)) {
                //noinspection deprecation
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
                if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                    sendErrorTr(player, "Unable to unban unknown player <player>.", unparsed("player", name, PRIMARY));
                    return true;
                }
                if (offlinePlayer.isOnline() && hasPermission(offlinePlayer.getPlayer(), "usb.exempt.ban")) {
                    sendErrorTr(offlinePlayer.getPlayer(), "<player> tried to ban you from their island!",
                        unparsed("player", player.getName()));
                    sendErrorTr(player, "<player> is exempt from being banned.", unparsed("player", name));
                    return true;
                }
                island.banPlayer(offlinePlayer, player);
                sendTr(player, "You banned <player> from warping to your island.",
                    unparsed("player", name, ERROR));
                if (offlinePlayer.isOnline()) {
                    sendTr(offlinePlayer.getPlayer(), "You have been <error>banned</error> from <leader>'s island.",
                        Placeholder.legacy("leader", player.getDisplayName(), PRIMARY));
                    if (plugin.locationIsOnIsland(player, offlinePlayer.getPlayer().getLocation())) {
                        plugin.getTeleportLogic().spawnTeleport(offlinePlayer.getPlayer(), true);
                    }
                }
            } else {
                //noinspection deprecation
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
                if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                    sendErrorTr(player, "Unable to unban unknown player <player>.", unparsed("player", name, PRIMARY));
                    return true;
                }
                island.unbanPlayer(offlinePlayer, player);
                sendTr(player, "You unbanned <player> from warping to your island.",
                    unparsed("player", name, SUCCESS));
                if (offlinePlayer.isOnline()) {
                    sendTr(offlinePlayer.getPlayer(), "You have been <success>unbanned</success> from <leader>'s island.",
                        Placeholder.legacy("leader", player.getDisplayName(), PRIMARY));
                }
            }
            WorldGuardHandler.updateRegion(island);
            return true;
        }
        return false;
    }
}
