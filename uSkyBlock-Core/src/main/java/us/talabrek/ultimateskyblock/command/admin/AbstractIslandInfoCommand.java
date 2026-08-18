package us.talabrek.ultimateskyblock.command.admin;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Arrays;
import java.util.Map;

import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;

/**
 * Command that lookup island info given the player name.
 */
public abstract class AbstractIslandInfoCommand extends AbstractPlayerInfoCommand {

    protected AbstractIslandInfoCommand(String name, String permission, String description) {
        super(name, permission, description);
    }

    protected abstract void doExecute(CommandSender sender, PlayerInfo playerInfo, IslandInfo islandInfo, String... args);

    @Override
    protected final void doExecute(CommandSender sender, PlayerInfo playerInfo) {
        // Not used
    }

    @Override
    protected void onMissingPlayerArgument(CommandSender sender) {
        // Handled in execute(): without a player name we can still resolve the island the sender
        // is standing on, and only report once that fallback has failed too.
    }

    @Override
    public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
        if (super.execute(sender, alias, data, args)) {
            PlayerInfo playerInfo = (PlayerInfo) data.get("playerInfo");
            if (playerInfo == null) {
                return false;
            }
            IslandInfo islandInfo = uSkyBlock.getInstance().getIslandInfo(playerInfo);
            if (islandInfo == null) {
                sendErrorTr(sender, "Player <player> has no island.", unparsed("player", playerInfo.getPlayerName(), PRIMARY));
                return false;
            }
            // super.execute() only succeeds when a player name was supplied, so args[0] is it.
            doExecute(sender, playerInfo, islandInfo, Arrays.copyOfRange(args, 1, args.length));
            return true;
        }
        if (args.length == 0 && sender instanceof Player player) {
            String islandName = WorldGuardHandler.getIslandNameAt(player.getLocation());
            IslandInfo islandInfo = islandName != null ? uSkyBlock.getInstance().getIslandInfo(islandName) : null;
            if (islandInfo != null) {
                doExecute(sender, null, islandInfo, args);
                return true;
            }
            sendErrorTr(sender, "You are not standing on an island. <muted>Supply a player name instead.");
            return false;
        }
        // A player name was supplied but did not resolve; super.execute() already said so.
        return false;
    }
}
