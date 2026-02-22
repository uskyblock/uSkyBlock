package us.talabrek.ultimateskyblock.command.admin;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.AbstractCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.uSkyBlock;
import static us.talabrek.ultimateskyblock.util.Msg.send;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

/**
 * Enables the user to toggle maintenance mode on and off.
 */
public class SetMaintenanceCommand extends AbstractCommand {
    private final uSkyBlock plugin;

    @Inject
    public SetMaintenanceCommand(@NotNull uSkyBlock plugin) {
        super("maintenance", "usb.admin.maintenance", "true|false", marktr("toggles maintenance mode"));
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
        if (sender instanceof ConsoleCommandSender) {
            if (args.length == 1 && args[0].matches("(true)|(false)")) {
                boolean maintenanceMode = Boolean.parseBoolean(args[0]);
                plugin.setMaintenanceMode(maintenanceMode);
                if (maintenanceMode) {
                    send(sender, tr("<error>MAINTENANCE:</error> <muted>Enabled. All uSkyBlock gameplay features are now disabled."));
                } else {
                    send(sender, tr("<error>MAINTENANCE:</error> <muted>Disabled. All uSkyBlock gameplay features are now operational."));
                }
            } else {
                return false;
            }
        } else {
            send(sender, tr("<error>Maintenance mode can only be changed from console!"));
        }
        return true;
    }
}
