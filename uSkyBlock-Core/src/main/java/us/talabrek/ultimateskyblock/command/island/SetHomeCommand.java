package us.talabrek.ultimateskyblock.command.island;

import com.google.inject.Inject;
import org.bukkit.entity.Player;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.LocationUtil;
import static us.talabrek.ultimateskyblock.util.Msg.send;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

public class SetHomeCommand extends RequireIslandCommand {

    @Inject
    public SetHomeCommand(uSkyBlock plugin) {
        super(plugin, "sethome|tpset", "usb.island.sethome", marktr("set the island home"));
    }

    @Override
    protected boolean doExecute(String alias, Player player, PlayerInfo pi, IslandInfo island, Map<String, Object> data, String... args) {
        if (!player.getWorld().getName().equalsIgnoreCase(plugin.getWorldManager().getWorld().getName())) {
            send(player, tr("<error>You must be closer to your island to set your skyblock home!"));
            return true;
        }
        if (!plugin.playerIsOnOwnIsland(player)) {
            send(player, tr("<error>You must be closer to your island to set your skyblock home!"));
            return true;
        }
        if (pi == null || !LocationUtil.isSafeLocation(player.getLocation())) {
            send(player, tr("<error>Your current location is not a safe home location."));
            return true;
        }

        pi.setHomeLocation(player.getLocation());
        pi.save();
        send(player, tr("<secondary>Your skyblock home has been set to your current location."));
        return true;
    }
}
