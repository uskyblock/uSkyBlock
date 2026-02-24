package us.talabrek.ultimateskyblock.command.island;

import com.google.inject.Inject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.api.event.InviteEvent;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.util.Msg.SECONDARY;
import static us.talabrek.ultimateskyblock.util.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.util.Msg.sendTr;

public class InviteCommand extends RequireIslandCommand {

    @Inject
    public InviteCommand(@NotNull uSkyBlock plugin) {
        super(plugin, "invite", "usb.party.invite", "oplayer", marktr("invite a player to your island"));
    }

    @Override
    protected boolean doExecute(String alias, Player player, PlayerInfo pi, IslandInfo island, Map<String, Object> data, String... args) {
        if (args.length == 0) {
            sendTr(player, "Use <cmd>/is invite [player]</cmd> to invite a player to your island.");
            if (!island.isParty()) {
                return true;
            }
            if (!island.isLeader(player) || !island.hasPerm(player, "canInviteOthers")) {
                sendErrorTr(player, "Only the island's owner can invite!");
                return true;
            }
            int diff = island.getMaxPartySize() - island.getPartySize();
            if (diff > 0) {
                sendTr(player, "You can invite <count> more players.",
                    SECONDARY, unparsed("count", String.valueOf(diff)));
            } else {
                sendErrorTr(player, "You can't invite any more players.");
            }
        }
        if (args.length == 1) {
            Player otherPlayer = Bukkit.getPlayer(args[0]);
            if (!island.hasPerm(player, "canInviteOthers")) {
                sendErrorTr(player, "You do not have permission to invite others to this island!");
                return true;
            }
            if (otherPlayer == null || !otherPlayer.isOnline()) {
                sendErrorTr(player, "That player is offline or doesn't exist.");
                return true;
            }
            if (player.getName().equalsIgnoreCase(otherPlayer.getName())) {
                sendErrorTr(player, "You can't invite yourself!");
                return true;
            }
            if (island.isLeader(otherPlayer)) {
                sendErrorTr(player, "That player is the leader of your island!");
                return true;
            }
            plugin.getServer().getPluginManager().callEvent(new InviteEvent(player, island, otherPlayer));
        }
        return true;
    }
}
