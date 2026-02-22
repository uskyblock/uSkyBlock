package dk.lockfuglsang.minecraft.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.UUID;

import static dk.lockfuglsang.minecraft.po.I18nUtil.trLegacy;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * Command delegator.
 */
public class BaseCommandExecutor extends CompositeCommand implements CommandExecutor {

    public BaseCommandExecutor(String name, String permission, String description) {
        super(name, permission, description);
    }

    public BaseCommandExecutor(String name, String permission, String params, String description) {
        super(name, permission, params, description);
    }
    public BaseCommandExecutor(String name, String permission, String params, String description, UUID... permissionOverrides) {
        super(name, permission, params, description, permissionOverrides);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!CommandManager.isRequirementsMet(sender, this, args)) {
            return true;
        }
        dk.lockfuglsang.minecraft.command.Command cmd = this;
        if (!hasAccess(cmd, sender)) {
            if (cmd != null) {
                sender.sendMessage(trLegacy("<error>You do not have access (<primary><permission></primary>)",
                    unparsed("permission", cmd.getPermission())));
            } else {
                sender.sendMessage(trLegacy("<error>Invalid command: <cmd><command></cmd>",
                    unparsed("command", alias)));
            }
            showUsage(sender, 1);
        } else {
            return execute(sender, alias, new HashMap<String, Object>(), args);
        }
        return true;
    }
}
