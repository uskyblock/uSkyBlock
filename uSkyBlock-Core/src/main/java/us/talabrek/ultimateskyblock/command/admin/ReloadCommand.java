package us.talabrek.ultimateskyblock.command.admin;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.AbstractCommand;
import org.bukkit.command.CommandSender;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static us.talabrek.ultimateskyblock.message.Msg.sendTr;

/**
 * Reloads the config-files for USB.
 */
public class ReloadCommand extends AbstractCommand {

    @Inject
    public ReloadCommand() {
        super("reload", "usb.admin.reload", marktr("reload configuration from file."));
    }

    @Override
    public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
        uSkyBlock.getInstance().reloadConfig();
        sendTr(sender, "Configuration reloaded from file.");
        return true;
    }
}
