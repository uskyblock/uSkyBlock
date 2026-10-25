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
            PlayerInfo playerInfo = findPlayer(sender, args[0]);
            if (playerInfo != null) {
                data.put("playerInfo", playerInfo);
                doExecute(sender, playerInfo);
                return true;
            }
        } else {
            onMissingPlayerArgument(sender);
        }
        return false;
    }

    /**
     * Looks up the player the command was given by name, or tells the sender there is no such player.
     *
     * <p>{@link AbstractIslandInfoCommand} looks the player up through this as well, and goes on to their island
     * instead of {@link #doExecute(CommandSender, PlayerInfo)}.</p>
     *
     * @return the player, or null once the sender has been told
     */
    protected PlayerInfo findPlayer(CommandSender sender, String playerName) {
        PlayerInfo playerInfo = uSkyBlock.getInstance().getPlayerInfo(playerName);
        if (playerInfo == null) {
            sendErrorTr(sender, "Invalid player <player> supplied.", unparsed("player", playerName, PRIMARY));
        }
        return playerInfo;
    }

    /**
     * Reports that no player argument was supplied.
     *
     * <p>Exposed so subclasses that intercept {@link #execute} can reuse the same message - and
     * the same msgid - once their own fallback has failed. {@link AbstractIslandInfoCommand} does
     * exactly that for senders with no location to fall back to.</p>
     */
    protected void onMissingPlayerArgument(CommandSender sender) {
        sendErrorTr(sender, "You must supply a player name.");
    }
}
