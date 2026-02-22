package us.talabrek.ultimateskyblock.command.admin;

import dk.lockfuglsang.minecraft.command.AbstractCommand;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.util.Msg.send;

/**
 * Allows transfer of leadership to another player.
 */
public class MakeLeaderCommand extends AbstractCommand {
    private final uSkyBlock plugin;

    public MakeLeaderCommand(@NotNull uSkyBlock plugin) {
        super("makeleader|transfer", "usb.admin.makeleader", "leader oplayer", marktr("transfer leadership to another player"));
        this.plugin = plugin;
    }

    @Override
    public boolean execute(final CommandSender sender, String alias, Map<String, Object> data, final String... args) {
        if (args.length == 2) {
            String islandPlayerName = args[0];
            String playerName = args[1];
            PlayerInfo currentLeader = plugin.getPlayerInfo(islandPlayerName);
            PlayerInfo newLeader = plugin.getPlayerInfo(playerName);

            if (currentLeader == null || !currentLeader.getHasIsland()) {
                send(sender, tr("<error>Player <player> has no island to transfer!",
                    unparsed("player", islandPlayerName)));
                return true;
            }
            IslandInfo islandInfo = plugin.getIslandInfo(currentLeader);
            if (islandInfo == null) {
                send(sender, tr("<error>Player <player> has no island to transfer!",
                    unparsed("player", islandPlayerName)));
                return true;
            }
            if (newLeader != null && newLeader.getHasIsland() && !newLeader.locationForParty().equals(islandInfo.getName())) {
                send(sender, tr("<error>Player <primary><player></primary> already has an island.</error> <muted>Use <cmd>/usb island remove [name]</cmd> first.",
                    unparsed("player", playerName)));
                return true;
            }
            newLeader.setJoinParty(islandInfo.getIslandLocation());
            Location homeLocation = currentLeader.getHomeLocation();
            islandInfo.removeMember(currentLeader); // Remove leader
            islandInfo.setupPartyLeader(newLeader.getPlayerName()); // Promote member
            islandInfo.addMember(currentLeader);
            newLeader.setHomeLocation(homeLocation);
            currentLeader.save();
            newLeader.save();
            WorldGuardHandler.updateRegion(islandInfo);
            plugin.getEventLogic().fireIslandLeaderChangedEvent(islandInfo, currentLeader, newLeader);
            islandInfo.sendMessageToIslandGroup(tr("<primary>Leadership transferred by <from><primary> to <to>",
                unparsed("from", sender.getName()),
                unparsed("to", playerName)));
            return true;
        }
        return false;
    }
}
