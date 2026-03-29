package us.talabrek.ultimateskyblock.command.island;

import com.google.inject.Inject;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.api.event.AcceptEvent;
import us.talabrek.ultimateskyblock.api.event.RejectEvent;
import us.talabrek.ultimateskyblock.command.InviteHandler;
import us.talabrek.ultimateskyblock.handler.ConfirmHandler;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;

public class AcceptRejectCommand extends RequirePlayerCommand {

    private final uSkyBlock plugin;
    private final InviteHandler inviteHandler;
    private final ConfirmHandler confirmHandler;

    @Inject
    public AcceptRejectCommand(@NotNull uSkyBlock plugin, @NotNull InviteHandler inviteHandler, @NotNull ConfirmHandler confirmHandler) {
        super("accept|reject", "usb.party.join", marktr("accept/reject an invitation."));
        this.plugin = plugin;
        this.inviteHandler = inviteHandler;
        this.confirmHandler = confirmHandler;
    }

    @Override
    protected boolean doExecute(String alias, Player player, Map<String, Object> data, String... args) {
        if (alias.equalsIgnoreCase("reject")) {
            plugin.getServer().getPluginManager().callEvent(new RejectEvent(player));
        } else if (alias.equalsIgnoreCase("accept")) {
            if (inviteHandler.acceptWouldDeleteIsland(player) && !confirmHandler.checkCommand(player, "/is accept")) {
                return true;
            }
            plugin.getServer().getPluginManager().callEvent(new AcceptEvent(player));
        }
        return true;
    }
}
