package us.talabrek.ultimateskyblock.command.island;

import com.google.inject.Inject;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import static dk.lockfuglsang.minecraft.po.I18nUtil.legacyArg;
import static dk.lockfuglsang.minecraft.po.I18nUtil.trLegacy;
import static us.talabrek.ultimateskyblock.util.Msg.send;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.parseMini;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

public class BanCommand extends RequireIslandCommand {

    @Inject
    public BanCommand(@NotNull uSkyBlock plugin) {
        super(plugin, "ban|unban", "usb.island.ban", "player", marktr("ban/unban a player from your island."));
        addFeaturePermission("usb.exempt.ban", trLegacy("exempts user from being banned"));
    }

    @Override
    protected boolean doExecute(String alias, Player player, PlayerInfo pi, IslandInfo island, Map<String, Object> data, String... args) {
        if (args.length == 0) {
            send(player, tr("The following players are banned from warping to your island:"));
            send(player, parseMini("<error><bans>", unparsed("bans", String.join(", ", island.getBans()))));
            send(player, tr("<muted>To ban or unban on your island, use <cmd>/is ban [player]</cmd>."));
            return true;
        } else if (args.length == 1) {
            String name = args[0];
            if (island.getMembers().contains(name)) {
                send(player, tr("<error>You can't ban members. Remove them first!"));
                return true;
            }
            if (!island.hasPerm(player, "canKickOthers")) {
                send(player, tr("<error>You do not have permission to kick/ban players."));
                return true;
            }
            if (!island.isBanned(name)) {
                //noinspection deprecation
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
                if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                    send(player, tr("<error>Unable to unban unknown player <primary><player></primary>.", unparsed("player", name)));
                    return true;
                }
                if (offlinePlayer.isOnline() && hasPermission(offlinePlayer.getPlayer(), "usb.exempt.ban")) {
                    send(offlinePlayer.getPlayer(), tr("<error><player> tried to ban you from their island!",
                        unparsed("player", player.getName())));
                    send(player, tr("<error><player> is exempt from being banned.", unparsed("player", name)));
                    return true;
                }
                island.banPlayer(offlinePlayer, player);
                send(player, tr("You banned <error><player></error> from warping to your island.",
                    unparsed("player", name)));
                if (offlinePlayer.isOnline()) {
                    send(offlinePlayer.getPlayer(), tr("You have been <error>banned</error> from <primary><leader></primary>'s island.",
                        legacyArg("leader", player.getDisplayName())));
                    if (plugin.locationIsOnIsland(player, offlinePlayer.getPlayer().getLocation())) {
                        plugin.getTeleportLogic().spawnTeleport(offlinePlayer.getPlayer(), true);
                    }
                }
            } else {
                //noinspection deprecation
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
                if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                    send(player, tr("<error>Unable to unban unknown player <primary><player></primary>.", unparsed("player", name)));
                    return true;
                }
                island.unbanPlayer(offlinePlayer, player);
                send(player, tr("You unbanned <secondary><player></secondary> from warping to your island.",
                    unparsed("player", name)));
                if (offlinePlayer.isOnline()) {
                    send(offlinePlayer.getPlayer(), tr("You have been <secondary>unbanned</secondary> from <primary><leader></primary>'s island.",
                        legacyArg("leader", player.getDisplayName())));
                }
            }
            WorldGuardHandler.updateRegion(island);
            return true;
        }
        return false;
    }
}
