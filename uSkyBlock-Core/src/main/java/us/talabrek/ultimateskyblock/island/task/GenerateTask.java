package us.talabrek.ultimateskyblock.island.task;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import us.talabrek.ultimateskyblock.Settings;
import us.talabrek.ultimateskyblock.handler.AsyncWorldEditHandler;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.Perk;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.player.PlayerPerk;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.Scheduler;
import static us.talabrek.ultimateskyblock.util.Msg.send;

import java.time.Duration;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

/**
 * Task for generating player-info-data after island has been formed.
 */
public class GenerateTask extends BukkitRunnable {
    private final uSkyBlock plugin;
    private final Scheduler scheduler;
    private final Player player;
    private final PlayerInfo pi;
    private final Location next;
    private final PlayerPerk playerPerk;
    private final String schematicName;
    boolean hasRun = false;
    private Location chestLocation;

    public GenerateTask(uSkyBlock plugin, final Player player, final PlayerInfo pi, final Location next, PlayerPerk playerPerk, String schematicName) {
        this.plugin = plugin;
        this.scheduler = plugin.getScheduler();
        this.player = player;
        this.pi = pi;
        this.next = next;
        this.playerPerk = playerPerk;
        this.schematicName = schematicName;
        chestLocation = null;
    }

    public void setChestLocation(Location chestLocation) {
        this.chestLocation = chestLocation;
    }

    @Override
    public void run() {
        if (hasRun) {
            return;
        }
        next.getChunk().load();
        Perk perk = plugin.getPerkLogic().getIslandPerk(schematicName).getPerk();
        perk = perk.combine(playerPerk.getPerk());
        if (chestLocation != null) {
            plugin.getIslandGenerator().setChest(chestLocation, perk);
        } else {
            plugin.getIslandGenerator().findAndSetChest(next, perk);
        }
        IslandInfo islandInfo = plugin.setNewPlayerIsland(pi, next);
        islandInfo.setSchematicName(schematicName);
        WorldGuardHandler.updateRegion(islandInfo);
        plugin.getCooldownHandler().resetCooldown(player, "restart", Settings.general_cooldownRestart);

        scheduler.sync(() -> {
                if (pi != null) {
                    pi.setIslandGenerating(false);
                }
                plugin.clearPlayerInventory(player);
                if (player != null && player.isOnline()) {
                    if (plugin.getConfig().getBoolean("options.restart.teleportWhenReady", true)) {
                        send(player, tr("<success>Congratulations!</success> Your island is ready."));
                        if (AsyncWorldEditHandler.isAWE()) {
                            send(player, tr("<muted>Note: Construction may still be in progress.</muted>"));
                        }
                        plugin.getTeleportLogic().homeTeleport(player, true);
                    } else {
                        send(player, tr("<success>Congratulations!</success> Your island is ready."),
                            tr("<muted>Use <cmd>/is h</cmd> or the <cmd>/is</cmd> menu to go there.</muted>"),
                            tr("<muted>Note: Construction may still be in progress.</muted>")
                        );
                    }
                }
                for (String command : plugin.getConfig().getStringList("options.restart.extra-commands")) {
                    plugin.execCommand(player, command, true);
                }
            }, Duration.ofMillis(plugin.getConfig().getInt("options.restart.teleportDelay", 2000))
        );
    }
}
