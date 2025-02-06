package us.talabrek.ultimateskyblock.command.admin;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.AbstractCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Map;
import java.util.UUID;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;

/**
 * Displays detailed version information.
 */
public class VersionCommand extends AbstractCommand {
    private final uSkyBlock plugin;

    @Inject
    public VersionCommand(@NotNull uSkyBlock plugin) {
        super("version|v", "usb.admin.version", null, marktr("displays version information"), null, UUID.fromString("97e8584c-438c-43cf-8b58-4e56c52398ed"));
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
        sender.sendMessage(plugin.getVersionInfo(true).split("\n"));
        return true;
    }

}
