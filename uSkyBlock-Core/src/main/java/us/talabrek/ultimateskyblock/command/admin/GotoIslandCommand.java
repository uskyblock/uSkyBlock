package us.talabrek.ultimateskyblock.command.admin;

import com.google.inject.Inject;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.message.Msg.sendPlayerOnly;
import static us.talabrek.ultimateskyblock.message.Msg.sendTr;

/**
 * Teleports to the player's island.
 */
public class GotoIslandCommand extends AbstractPlayerInfoCommand {
    private final uSkyBlock plugin;

    @Inject
    public GotoIslandCommand(@NotNull uSkyBlock plugin) {
        super("goto", "usb.mod.goto", marktr("teleport to another player's island"));
        this.plugin = plugin;
    }

    @Override
    protected void doExecute(final CommandSender sender, final PlayerInfo playerInfo) {
        if (!(sender instanceof Player player)) {
            sendPlayerOnly(sender);
            return;
        }
        if (!playerInfo.getHasIsland()) {
            sendErrorTr(sender, "That player does not have an island!");
        } else if (playerInfo.getHomeLocation() != null) {
            sendTr(sender, "Teleporting you to <player>'s island.", unparsed("player", playerInfo.getPlayerName()));
            plugin.getTeleportLogic().safeTeleport(player, playerInfo.getHomeLocation(), true);
        } else if (playerInfo.getIslandLocation() != null) {
            sendTr(sender, "Teleporting you to <player>'s island.", unparsed("player", playerInfo.getPlayerName()));
            plugin.getTeleportLogic().safeTeleport(player, playerInfo.getIslandLocation(), true);
        } else {
            sendErrorTr(sender, "That player does not have an island!");
        }
    }
}
