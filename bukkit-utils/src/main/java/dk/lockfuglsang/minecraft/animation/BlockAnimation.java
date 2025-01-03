package dk.lockfuglsang.minecraft.animation;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Sends (bogus) block-info to the player
 */
public class BlockAnimation implements Animation {
    private final Player player;
    private final List<Location> points;
    private final BlockData blockData;
    private volatile boolean shown;

    public BlockAnimation(Player player, List<Location> points, BlockData blockData) {
        this.player = player;
        this.points = points;
        this.blockData = blockData;
        shown = false;
    }

    @Override
    public boolean show() {
        if (shown) {
            return true;
        }
        if (!player.isOnline()) {
            return false;
        }
        for (Location loc : points) {
            player.sendBlockChange(loc, blockData);
        }
        shown = true;
        return true;
    }

    @Override
    public boolean hide() {
        if (shown) {
            shown = false;
            player.sendBlockChanges(points.stream().map(loc -> loc.getBlock().getState()).toList());
            return true;
        }
        return false;
    }

    @Override
    public Player getPlayer() {
        return player;
    }
}
