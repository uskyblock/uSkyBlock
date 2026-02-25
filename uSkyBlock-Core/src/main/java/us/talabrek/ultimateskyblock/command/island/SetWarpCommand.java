package us.talabrek.ultimateskyblock.command.island;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.po.I18nUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;

public class SetWarpCommand extends RequireIslandCommand {

    @Inject
    public SetWarpCommand(@NotNull uSkyBlock plugin) {
        super(plugin, "setwarp|warpset", "usb.island.setwarp", marktr("set your island's warp location"));
    }

    @Override
    protected boolean doExecute(String alias, Player player, PlayerInfo pi, IslandInfo island, Map<String, Object> data, String... args) {
        if (!island.hasPerm(player, "canChangeWarp")) {
            sendErrorTr(player, "You do not have permission to set your island's warp point!");
        } else if (!plugin.playerIsOnOwnIsland(player)) {
            sendErrorTr(player, "You need to be on your own island to set the warp!");
        } else {
            island.setWarpLocation(player.getLocation());
            island.sendMessageToIslandGroup(I18nUtil.tr("<player> changed the island warp location.",
                unparsed("player", player.getName(), PRIMARY)));
        }
        return true;
    }
}
