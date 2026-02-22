package us.talabrek.ultimateskyblock.command.admin;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.AbstractCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.util.Msg.send;

/**
 * Registers an island to a player.
 */
public class RegisterIslandToPlayerCommand extends AbstractCommand {

    @Inject
    public RegisterIslandToPlayerCommand() {
        super("register", "usb.admin.register", "player", marktr("set a player's island to your location"));
    }

    @Override
    public boolean execute(final CommandSender sender, String alias, Map<String, Object> data, final String... args) {
        if (!(sender instanceof Player player)) {
            return false;
        }
        if (args.length < 1) {
            return false;
        }
        String playerName = args[0];
        if (uSkyBlock.getInstance().devSetPlayerIsland(player, player.getLocation(), playerName)) {
            send(sender, tr("<secondary>Set <player>'s island to the current island.", unparsed("player", playerName)));
        } else {
            send(sender, tr("<error>Island not found: unable to set the island!"));
        }
        return true;
    }
}
