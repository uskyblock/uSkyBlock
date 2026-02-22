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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.legacyArg;
import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.parseMini;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.util.Msg.send;

public class TrustCommand extends RequireIslandCommand {

    @Inject
    public TrustCommand(@NotNull uSkyBlock plugin) {
        super(plugin, "trust|untrust", "usb.island.trust", "?player", marktr("trust/untrust a player to help on your island."));
    }

    @Override
    protected boolean doExecute(final String alias, final Player player, final PlayerInfo pi, final IslandInfo island, Map<String, Object> data, String... args) {
        if (args.length == 0) {
            send(player, tr("The following players are trusted on your island:"));
            send(player, parseMini("<primary><players>", unparsed("players", String.join(", ", island.getTrustees()))));
            send(player, tr("The following leaders trust you:"));
            send(player, parseMini("<primary><leaders>", unparsed("leaders", String.join(", ", getLeaderNames(pi)))));
            send(player, tr("<muted>To trust or untrust on your island, use <cmd>/is trust [player]</cmd>.</muted>"));
            return true;
        } else if (args.length == 1) {
            final String name = args[0];
            if (island.getMembers().contains(name)) {
                send(player, tr("<error>Members are already trusted!"));
                return true;
            }
            //noinspection deprecation
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
            if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                send(player, tr("<error>Unknown player <player>", unparsed("player", name)));
                return true;
            }
            if (alias.equals("trust")) {
                island.trustPlayer(offlinePlayer, player);
                if (offlinePlayer.isOnline()) {
                    send(offlinePlayer.getPlayer(), tr("You are now trusted on <primary><leader></primary>'s island.",
                        legacyArg("leader", pi.getDisplayName())));
                }
                island.sendMessageToIslandGroup(tr("<primary><player></primary> trusted <primary><target></primary> on the island.",
                    unparsed("player", player.getName()),
                    unparsed("target", name)));
            } else {
                island.untrustPlayer(offlinePlayer, player);
                if (offlinePlayer.isOnline()) {
                    send(offlinePlayer.getPlayer(), tr("You are no longer trusted on <primary><leader></primary>'s island.",
                        legacyArg("leader", pi.getDisplayName())));
                }
                island.sendMessageToIslandGroup(tr("<primary><player></primary> revoked trust for <primary><target></primary> on the island.",
                    unparsed("player", player.getName()),
                    unparsed("target", name)));
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
