package us.talabrek.ultimateskyblock.signs;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.util.FormatUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.uSkyBlock;

/**
 * Handles USB Signs
 */
@Singleton
public class SignEvents implements Listener {
    private final uSkyBlock plugin;
    private final SignLogic logic;

    @Inject
    public SignEvents(@NotNull uSkyBlock plugin, @NotNull SignLogic logic) {
        this.plugin = plugin;
        this.logic = logic;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerHitSign(PlayerInteractEvent e) {
        if (e.useInteractedBlock() == Event.Result.DENY
                || (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.LEFT_CLICK_BLOCK)
                || e.getClickedBlock() == null
                || !(e.getClickedBlock().getState().getBlockData() instanceof WallSign)
                || !e.getPlayer().hasPermission("usb.island.signs.use")
                || !plugin.getWorldManager().isSkyAssociatedWorld(e.getPlayer().getWorld())
                || !(plugin.playerIsOnOwnIsland(e.getPlayer()))
                ) {
            return;
        }
        if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            logic.updateSign(e.getClickedBlock().getLocation());
        } else {
            logic.signClicked(e.getPlayer(), e.getClickedBlock().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSignChanged(SignChangeEvent e) {
        if (!plugin.getWorldManager().isSkyAssociatedWorld(e.getPlayer().getWorld())
                || !e.getLines()[0].equalsIgnoreCase("[usb]")
                || e.getLines()[1].trim().isEmpty()
                || !e.getPlayer().hasPermission("usb.island.signs.place")
                || !(e.getBlock().getState() instanceof Sign sign)
                ) {
            return;
        }

        if (sign.getBlock().getState().getBlockData() instanceof WallSign data) {
            BlockFace attached = data.getFacing().getOppositeFace();
            Block wallBlock = sign.getBlock().getRelative(attached);
            if (isChest(wallBlock)) {
                logic.addSign(sign, e.getLines(), (Chest) wallBlock.getState());
            }
        }
    }

    private boolean isChest(Block wallBlock) {
        return wallBlock != null
                && (wallBlock.getType() == Material.CHEST || wallBlock.getType() == Material.TRAPPED_CHEST)
                && wallBlock.getState() instanceof Chest;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryMovedEvent(InventoryMoveItemEvent e) {
        if (e.getDestination().getLocation() == null
                || !plugin.getWorldManager().isSkyAssociatedWorld(e.getDestination().getLocation().getWorld())) {
            return;
        }
        if (e.getDestination().getType() == InventoryType.CHEST) {
            Location loc = e.getDestination().getLocation();
            if (loc != null) {
                logic.updateSignsOnContainer(loc);
            }
        }
        if (e.getSource().getType() == InventoryType.CHEST) {
            Location loc = e.getSource().getLocation();
            if (loc != null) {
                logic.updateSignsOnContainer(loc);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChestClosed(InventoryCloseEvent e) {
        if (!plugin.getWorldManager().isSkyAssociatedWorld(e.getPlayer().getLocation().getWorld())
                || e.getInventory().getType() != InventoryType.CHEST
                ) {
            return;
        }
        Location loc = e.getInventory().getLocation();
        if (loc != null) {
            logic.updateSignsOnContainer(loc);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSignOrChestBreak(BlockBreakEvent e) {
        if ((!(e.getBlock().getState().getBlockData() instanceof WallSign) &&
                    !(e.getBlock().getType() == Material.CHEST || e.getBlock().getType() == Material.TRAPPED_CHEST))
                || !plugin.getWorldManager().isSkyAssociatedWorld(e.getBlock().getLocation().getWorld())
                ) {
            return;
        }
        if (e.getBlock().getState().getBlockData() instanceof WallSign) {
            logic.removeSign(e.getBlock().getLocation());
        } else {
            logic.removeChest(e.getBlock().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockHit(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getClickedBlock() == null
                || event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getPlayer().getGameMode() != GameMode.SURVIVAL
                || !(event.getClickedBlock().getState() instanceof Sign sign)
                || !player.hasPermission("usb.island.signs.use")
                || !plugin.getWorldManager().isSkyAssociatedWorld(player.getWorld())) {
            return;
        }

        String firstLine = FormatUtil.stripFormatting(sign.getLine(0)).trim();
        if (firstLine.startsWith("/")) {
            event.setCancelled(true);
            player.performCommand(firstLine.substring(1));
        }
    }
}
