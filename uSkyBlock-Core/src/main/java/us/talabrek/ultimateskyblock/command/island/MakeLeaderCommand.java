package us.talabrek.ultimateskyblock.command.island;

import com.google.inject.Inject;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.legacyArg;
import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.util.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.util.Msg.sendTr;

public class MakeLeaderCommand extends RequireIslandCommand {

    @Inject
    public MakeLeaderCommand(@NotNull uSkyBlock plugin) {
        super(plugin, "makeleader|transfer", "usb.island.makeleader", "member", marktr("transfer leadership to another member"));
    }

    @Override
    protected boolean doExecute(String alias, Player player, PlayerInfo currentLeader, IslandInfo island, Map<String, Object> data, String... args) {
        if (args.length == 1) {
            String newLeader = args[0];
            if (!island.getMembers().contains(newLeader)) {
                sendErrorTr(player, "You can only transfer ownership to party members!");
                return true;
            }
            if (island.getLeader().equals(newLeader)) {
                sendTr(player, "<primary><player></primary> is already the leader of your island.", unparsed("player", newLeader));
                return true;
            }
            if (!island.isLeader(player)) {
                sendErrorTr(player, "Only the island leader can transfer leadership!");
                island.sendMessageToIslandGroup(tr("<player> tried to take over the island!",
                    unparsed("player", newLeader)));
                return true;
            }
            island.setupPartyLeader(newLeader); // Promote member
            island.setupPartyMember(currentLeader); // Demote leader
            WorldGuardHandler.updateRegion(island);
            PlayerInfo newLeaderInfo = uSkyBlock.getInstance().getPlayerInfo(newLeader);
            uSkyBlock.getInstance().getEventLogic().fireIslandLeaderChangedEvent(island, currentLeader, newLeaderInfo);
            island.sendMessageToIslandGroup(tr("Leadership transferred by <primary><from></primary> to <primary><to></primary>.",
                legacyArg("from", player.getDisplayName()),
                unparsed("to", newLeader)));
            return true;
        }
        return false;
    }
}
