package us.talabrek.ultimateskyblock.command.island;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.po.I18nUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.api.event.InviteEvent;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import static us.talabrek.ultimateskyblock.util.Msg.send;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

public class InviteCommand extends RequireIslandCommand {

    @Inject
    public InviteCommand(@NotNull uSkyBlock plugin) {
        super(plugin, "invite", "usb.party.invite", "oplayer", marktr("invite a player to your island"));
    }

    @Override
    protected boolean doExecute(String alias, Player player, PlayerInfo pi, IslandInfo island, Map<String, Object> data, String... args) {
        if (args.length == 0) {
            send(player, I18nUtil.tr("Use <cmd>/is invite [player]</cmd> to invite a player to your island."));
            if (!island.isParty()) {
                return true;
            }
            if (!island.isLeader(player) || !island.hasPerm(player, "canInviteOthers")) {
                send(player, I18nUtil.tr("<error>Only the island's owner can invite!"));
                return true;
            }
            int diff = island.getMaxPartySize() - island.getPartySize();
            if (diff > 0) {
                send(player, I18nUtil.tr("<secondary>You can invite <count> more players.",
                    unparsed("count", String.valueOf(diff))));
            } else {
                send(player, I18nUtil.tr("<error>You can't invite any more players."));
            }
        }
        if (args.length == 1) {
            Player otherPlayer = Bukkit.getPlayer(args[0]);
            if (!island.hasPerm(player, "canInviteOthers")) {
                send(player, I18nUtil.tr("<error>You do not have permission to invite others to this island!"));
                return true;
            }
            if (otherPlayer == null || !otherPlayer.isOnline()) {
                send(player, I18nUtil.tr("<error>That player is offline or doesn't exist."));
                return true;
            }
            if (player.getName().equalsIgnoreCase(otherPlayer.getName())) {
                send(player, I18nUtil.tr("<error>You can't invite yourself!"));
                return true;
            }
            if (island.isLeader(otherPlayer)) {
                send(player, I18nUtil.tr("<error>That player is the leader of your island!"));
                return true;
            }
            plugin.getServer().getPluginManager().callEvent(new InviteEvent(player, island, otherPlayer));
        }
        return true;
    }
}
