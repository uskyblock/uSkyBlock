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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.parseMini;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.util.Msg.MUTED;
import static us.talabrek.ultimateskyblock.util.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.util.Msg.send;
import static us.talabrek.ultimateskyblock.util.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.util.Msg.sendTr;

public class TrustCommand extends RequireIslandCommand {

    @Inject
    public TrustCommand(@NotNull uSkyBlock plugin) {
        super(plugin, "trust|untrust", "usb.island.trust", "?player", marktr("trust/untrust a player to help on your island."));
    }

    @Override
    protected boolean doExecute(final String alias, final Player player, final PlayerInfo pi, final IslandInfo island, Map<String, Object> data, String... args) {
        if (args.length == 0) {
            sendTr(player, "The following players are trusted on your island:");
            send(player, parseMini("<primary><players>", unparsed("players", String.join(", ", island.getTrustees()))));
            sendTr(player, "The following leaders trust you:");
            send(player, parseMini("<primary><leaders>", unparsed("leaders", String.join(", ", getLeaderNames(pi)))));
            sendTr(player, "To trust or untrust on your island, use <cmd>/is trust [player]</cmd>.", MUTED);
            return true;
        } else if (args.length == 1) {
            final String name = args[0];
            if (island.getMembers().contains(name)) {
                sendErrorTr(player, "Members are already trusted!");
                return true;
            }
            //noinspection deprecation
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
            if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                sendErrorTr(player, "Unknown player <player>", unparsed("player", name));
                return true;
            }
            if (alias.equals("trust")) {
                island.trustPlayer(offlinePlayer, player);
                if (offlinePlayer.isOnline()) {
                    sendTr(offlinePlayer.getPlayer(), "You are now trusted on <leader>'s island.",
                        Placeholder.legacy("leader", pi.getDisplayName(), PRIMARY));
                }
                island.sendMessageToIslandGroup(tr("<player> trusted <target> on the island.",
                    unparsed("player", player.getName(), PRIMARY),
                    unparsed("target", name, PRIMARY)));
            } else {
                island.untrustPlayer(offlinePlayer, player);
                if (offlinePlayer.isOnline()) {
                    sendTr(offlinePlayer.getPlayer(), "You are no longer trusted on <leader>'s island.",
                        Placeholder.legacy("leader", pi.getDisplayName(), PRIMARY));
                }
                island.sendMessageToIslandGroup(tr("<player> revoked trust for <target> on the island.",
                    unparsed("player", player.getName(), PRIMARY),
                    unparsed("target", name, PRIMARY)));
            }
            WorldGuardHandler.updateRegion(island);
            return true;
        }
        return false;
    }

    private List<String> getLeaderNames(PlayerInfo pi) {
        List<String> trustedOn = pi.getTrustedOn();
        List<String> leaderNames = new ArrayList<>();
        for (String islandName : trustedOn) {
            us.talabrek.ultimateskyblock.api.IslandInfo islandInfo = plugin.getIslandInfo(islandName);
            if (islandInfo != null && islandInfo.getLeader() != null) {
                leaderNames.add(islandInfo.getLeader());
            }
        }
        return leaderNames;
    }
}
