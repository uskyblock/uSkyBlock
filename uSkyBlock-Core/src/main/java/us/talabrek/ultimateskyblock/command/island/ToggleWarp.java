package us.talabrek.ultimateskyblock.command.island;

import com.google.inject.Inject;
import org.bukkit.entity.Player;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.util.Msg.sendErrorTr;

public class ToggleWarp extends RequireIslandCommand {

    @Inject
    public ToggleWarp(uSkyBlock plugin) {
        super(plugin, "togglewarp|tw", "usb.island.togglewarp", marktr("enable/disable warping to your island."));
    }

    @Override
    protected boolean doExecute(String alias, Player player, PlayerInfo pi, IslandInfo island, Map<String, Object> data, String... args) {
        if (island.hasPerm(player, "canToggleWarp")) {
            if (!island.hasWarp()) {
                if (island.isLocked()) {
                    sendErrorTr(player, "Your island is locked. You must unlock it before enabling your warp.");
                    return true;
                }
                island.sendMessageToIslandGroup(tr("<primary><player></primary> activated the island warp.",
                    unparsed("player", player.getName())));
                island.setWarp(true);
            } else {
                island.sendMessageToIslandGroup(tr("<primary><player></primary> deactivated the island warp.",
                    unparsed("player", player.getName())));
                island.setWarp(false);
            }
        } else {
            sendErrorTr(player, "You do not have permission to enable/disable your island's warp!");
        }
        return true;
    }
}
