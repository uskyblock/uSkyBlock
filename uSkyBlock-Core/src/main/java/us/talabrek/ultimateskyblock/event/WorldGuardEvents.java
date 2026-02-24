package us.talabrek.ultimateskyblock.event;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static us.talabrek.ultimateskyblock.util.Msg.ERROR;

/**
 * Replacement for the WG ENTRY/EXIT deny flags.
 */
@Singleton
public class WorldGuardEvents implements Listener {
    private final uSkyBlock plugin;

    @Inject
    public WorldGuardEvents(@NotNull uSkyBlock plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerMove(PlayerMoveEvent e) {
        if (e.getTo() == null || !plugin.getWorldManager().isSkyAssociatedWorld(e.getTo().getWorld())) {
            return;
        }
        String islandNameAt = WorldGuardHandler.getIslandNameAt(e.getTo());
        if (islandNameAt == null) {
            return;
        }
        IslandInfo islandInfo = plugin.getIslandInfo(islandNameAt);
        if (islandInfo == null || islandInfo.getBans().isEmpty()) {
            return;
        }
        Player player = e.getPlayer();
        if (!player.isOp() && !player.hasPermission("usb.mod.bypassprotection") && isBlockedFromEntry(player, islandInfo)) {
            e.setCancelled(true);
            Location l = e.getTo().clone();
            l.subtract(islandInfo.getIslandLocation());
            Vector v = new Vector(l.getX(), l.getY(), l.getZ());
            v.normalize();
            v.multiply(1.5); // Bounce
            player.setVelocity(v);
            if (islandInfo.isBanned(player)) {
                plugin.notifyPlayer(player, tr("Banned: You are banned from this island.", ERROR));
            } else {
                plugin.notifyPlayer(player, tr("Locked: That island is locked. No entry allowed.", ERROR));
            }
        }
    }

    private boolean isBlockedFromEntry(Player player, IslandInfo islandInfo) {
        return islandInfo.isBanned(player) || (islandInfo.isLocked() && !(
                islandInfo.getMembers().contains(player.getName()) ||
                        islandInfo.getTrusteeUUIDs().contains(player.getUniqueId())
                ));
    }
}
