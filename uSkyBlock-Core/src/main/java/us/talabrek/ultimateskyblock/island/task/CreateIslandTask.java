package us.talabrek.ultimateskyblock.island.task;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import us.talabrek.ultimateskyblock.player.PlayerPerk;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.time.Duration;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

/**
 * Task instead of anonymous runnable - so we get some info on /timings paste
 */
public class CreateIslandTask extends BukkitRunnable {
    private final uSkyBlock plugin;
    private final Player player;
    private final PlayerPerk playerPerk;
    private final Location next;
    private final String cSchem;
    private final Scheduler scheduler;

    public CreateIslandTask(uSkyBlock plugin, Player player, PlayerPerk playerPerk, Location next, String cSchem) {
        this.plugin = plugin;
        this.scheduler = plugin.getScheduler();
        this.player = player;
        this.playerPerk = playerPerk;
        this.next = next;
        this.cSchem = cSchem;
    }

    @Override
    public void run() {
        if (!plugin.getIslandGenerator().createIsland(playerPerk, next, cSchem)) {
            player.sendMessage(tr("Unable to locate schematic {0}, contact a server-admin", cSchem));
        }
        GenerateTask generateTask = new GenerateTask(plugin, player, playerPerk.getPlayerInfo(), next, playerPerk, cSchem);
        Duration heartBeat = Duration.ofMillis(plugin.getConfig().getInt("asyncworldedit.watchDog.heartBeatMs", 2000));
        final BukkitRunnable completionWatchDog = new LocateChestTask(plugin, player, next, generateTask);
        scheduler.sync(completionWatchDog, Duration.ZERO, heartBeat);
    }
}
