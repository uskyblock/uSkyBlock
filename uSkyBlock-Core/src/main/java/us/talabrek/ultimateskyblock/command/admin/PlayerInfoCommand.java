package us.talabrek.ultimateskyblock.command.admin;

import com.google.inject.Inject;
import org.bukkit.command.CommandSender;
import us.talabrek.ultimateskyblock.player.PlayerInfo;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;

public class PlayerInfoCommand extends AbstractPlayerInfoCommand {

    @Inject
    public PlayerInfoCommand() {
        super("info", "usb.admin.info", marktr("show player-information"));
    }

    @Override
    protected void doExecute(CommandSender sender, PlayerInfo playerInfo) {
        sender.sendMessage(playerInfo.toString());
    }
}
