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
        // execute() is overridden and routes to the island-aware doExecute below, so the parent's
        // player-only variant is never reached. Fail loudly rather than silently doing nothing if
        // that ever stops being true.
        throw new UnsupportedOperationException(
            getClass().getName() + " routes through doExecute(CommandSender, PlayerInfo, IslandInfo, String...)");
    }

    @Override
    public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
        if (args.length == 0) {
            // No player name given. A Player can still be resolved from where they stand; any
            // other sender has no location to fall back to.
            if (sender instanceof Player player) {
                String islandName = WorldGuardHandler.getIslandNameAt(player.getLocation());
                IslandInfo islandInfo = islandName != null ? uSkyBlock.getInstance().getIslandInfo(islandName) : null;
                if (islandInfo != null) {
                    doExecute(sender, null, islandInfo, args);
                    return true;
                }
                sendErrorTr(sender, "You are not standing on an island. <muted>Supply a player name instead.");
            } else {
                onMissingPlayerArgument(sender);
            }
            return false;
        }
        if (super.execute(sender, alias, data, args)) {
            // super.execute() only succeeds after putting a non-null playerInfo in data, and only
            // when a player name was supplied - so args[0] is that name.
            PlayerInfo playerInfo = (PlayerInfo) data.get("playerInfo");
            if (playerInfo == null) {
                throw new IllegalStateException(
                    "AbstractPlayerInfoCommand.execute returned true without storing a playerInfo");
            }
            IslandInfo islandInfo = uSkyBlock.getInstance().getIslandInfo(playerInfo);
            if (islandInfo == null) {
                sendErrorTr(sender, "Player <player> has no island.", unparsed("player", playerInfo.getPlayerName(), PRIMARY));
                return false;
            }
            doExecute(sender, playerInfo, islandInfo, Arrays.copyOfRange(args, 1, args.length));
            return true;
        }
        // A player name was supplied but did not resolve; super.execute() already said so.
        return false;
    }
}
