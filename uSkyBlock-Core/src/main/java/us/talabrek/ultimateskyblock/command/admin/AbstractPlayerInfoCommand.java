package us.talabrek.ultimateskyblock.command.admin;

import dk.lockfuglsang.minecraft.command.AbstractCommand;
import org.bukkit.command.CommandSender;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Map;

import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;

/**
 * Command that has <code>player</code> as first argument, and uses playerInfo.
 */
public abstract class AbstractPlayerInfoCommand extends AbstractCommand {
    protected AbstractPlayerInfoCommand(String name, String permission, String description) {
        super(name, permission, "player", description);
    }
    protected abstract void doExecute(CommandSender sender, PlayerInfo playerInfo);
    @Override
    public boolean execute(final CommandSender sender, String alias, final Map<String, Object> data, final String... args) {
        if (args.length > 0) {
            String playerName = args[0];
            PlayerInfo playerInfo = uSkyBlock.getInstance().getPlayerInfo(playerName);
            if (playerInfo != null) {
                data.put("playerInfo", playerInfo);
                doExecute(sender, playerInfo);
                return true;
            }
            sendErrorTr(sender, "Invalid player <player> supplied.", unparsed("player", args[0], PRIMARY));
        } else {
            onMissingPlayerArgument(sender);
        }
        return false;
    }

    /**
     * Called when no player argument was supplied.
     *
     * <p>The default tells the sender what is missing. Subclasses that can still resolve a target
     * another way - see {@link AbstractIslandInfoCommand}, which falls back to the island the
     * sender is standing on - override this to stay quiet here and report for themselves once
     * their own fallback has also failed.</p>
     */
    protected void onMissingPlayerArgument(CommandSender sender) {
        sendErrorTr(sender, "You must supply a player name.");
    }
}
