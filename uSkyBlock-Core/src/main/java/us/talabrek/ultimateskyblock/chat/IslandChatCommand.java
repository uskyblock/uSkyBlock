package us.talabrek.ultimateskyblock.chat;

import dk.lockfuglsang.minecraft.command.BaseCommandExecutor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import us.talabrek.ultimateskyblock.api.event.IslandChatEvent;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.trLegacy;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.util.Msg.MUTED;
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
                    sendTr(player, "Toggled <primary><chat-type></primary> chat <success>on</success>.", unparsed("chat-type", chatType));
                    sendTr(player, "Repeat <cmd><command></cmd> to toggle it off.",
                        MUTED, unparsed("command", "/" + alias));
                } else {
                    sendTr(player, "Toggled <primary><chat-type></primary> chat off.", unparsed("chat-type", chatType));
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
