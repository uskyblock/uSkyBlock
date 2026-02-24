package us.talabrek.ultimateskyblock.chat;

import dk.lockfuglsang.minecraft.command.BaseCommandExecutor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import us.talabrek.ultimateskyblock.api.event.IslandChatEvent;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.trLegacy;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.util.Msg.CMD;
import static us.talabrek.ultimateskyblock.util.Msg.MUTED;
import static us.talabrek.ultimateskyblock.util.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.util.Msg.sendPlayerOnly;
import static us.talabrek.ultimateskyblock.util.Msg.sendTr;

/**
 * The chat command for party messages
 */
public abstract class IslandChatCommand extends BaseCommandExecutor {
    private final uSkyBlock plugin;
    private final ChatLogic chatLogic;

    public IslandChatCommand(uSkyBlock plugin, ChatLogic chatLogic, String name, String permission, String description) {
        super(name, permission, "?message", description);
        this.plugin = plugin;
        this.chatLogic = chatLogic;
    }

    @Override
    public String getUsage() {
        return trLegacy("Either send a message directly to your group, or toggle it on/off.");
    }

    @Override
    public boolean execute(CommandSender commandSender, String alias, Map<String, Object> data, String... args) {
        if (!plugin.isRequirementsMet(commandSender, this, args)) {
            return true;
        }
        if (commandSender instanceof Player player) {
            IslandChatEvent.Type type = this instanceof PartyTalkCommand ? IslandChatEvent.Type.PARTY : IslandChatEvent.Type.ISLAND;
            if (args == null || args.length == 0) {
                String chatType = type == IslandChatEvent.Type.PARTY
                    ? trLegacy("party")
                    : trLegacy("island");
                if (chatLogic.toggle(player, type)) {
                    sendTr(player, "Toggled <chat-type> chat <success>on</success>.", unparsed("chat-type", chatType, PRIMARY));
                    sendTr(player, "Repeat <command> to toggle it off.",
                        MUTED, unparsed("command", "/" + alias, CMD));
                } else {
                    sendTr(player, "Toggled <chat-type> chat off.", unparsed("chat-type", chatType, PRIMARY));
                }
                return true;
            } else if (args != null && args.length == 1 && (args[0].equalsIgnoreCase("?") || args[0].equalsIgnoreCase("help"))) {
                showUsage(commandSender, 1);
                return true;
            }
            String message = String.join(" ", args);
            Bukkit.getServer().getPluginManager().callEvent(new IslandChatEvent(player, type, message));
        } else {
            sendPlayerOnly(commandSender);
        }
        return true;
    }
}
