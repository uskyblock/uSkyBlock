package us.talabrek.ultimateskyblock.event;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.HashMap;
import java.util.Map;

/**
 * Events triggering the tool-menu
 */
@Singleton
public class ToolMenuEvents implements Listener {
    private final uSkyBlock plugin;
    private final ItemStack tool;
    private final Map<String, String> commandMap = new HashMap<>();
    private final RuntimeConfigs runtimeConfigs;

    @Inject
    public ToolMenuEvents(@NotNull uSkyBlock plugin, @NotNull RuntimeConfigs runtimeConfigs) {
        this.plugin = plugin;
        this.runtimeConfigs = runtimeConfigs;
        RuntimeConfig.ToolMenu toolMenu = runtimeConfigs.current().toolMenu();
        tool = toolMenu.tool().create();
        registerCommands();
    }

    private void registerCommands() {
        for (RuntimeConfig.ToolMenuCommand entry : runtimeConfigs.current().toolMenu().commands()) {
            ItemStack item = entry.item().create();
            if (item.getType().isBlock()) {
                commandMap.put(item.getType().toString(), entry.command());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockHit(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null || e.getAction() != Action.LEFT_CLICK_BLOCK ||
            e.getPlayer().getGameMode() != GameMode.SURVIVAL) {
            return;
        }
        Player player = e.getPlayer();
        if (!plugin.getWorldManager().isSkyAssociatedWorld(player.getWorld()) || !isTool(e.getItem())) {
            return;
        }

        // We are in a skyworld, a block has been hit, with the tool
        Material block = e.getClickedBlock().getType();
        if (commandMap.containsKey(block.toString())) {
            doCmd(e, player, block.toString());
        }
    }

    private void doCmd(PlayerInteractEvent e, Player player, String itemId) {
        String command = commandMap.get(itemId);
        e.setCancelled(true);
        plugin.execCommand(player, command, false);
    }

    /**
     * Checks if the given {@link ItemStack} is the configured tool for the tool menu.
     *
     * @param item ItemStack to check.
     * @return True if it is the configured tool, false otherwise.
     */
    private boolean isTool(ItemStack item) {
        return item != null && tool != null && item.getType() == tool.getType();
    }
}
