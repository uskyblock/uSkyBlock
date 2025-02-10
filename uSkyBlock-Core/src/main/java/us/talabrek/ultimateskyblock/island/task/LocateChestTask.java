package us.talabrek.ultimateskyblock.island.task;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import dk.lockfuglsang.minecraft.util.TimeUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.LocationUtil;
import us.talabrek.ultimateskyblock.util.Scheduler;

/**
 * A task that looks for a chest at an island location.
 */
public class LocateChestTask extends BukkitRunnable {
    private final Player player;
    private final Location islandLocation;
    private final GenerateTask onCompletion;
    private final Scheduler scheduler;
    private final long timeout;

    private long tStart;

    public LocateChestTask(uSkyBlock plugin, Player player, Location islandLocation, GenerateTask onCompletion) {
        this.scheduler = plugin.getScheduler();
        this.player = player;
        this.islandLocation = islandLocation;
        this.onCompletion = onCompletion;
        timeout = System.currentTimeMillis() + TimeUtil.stringAsMillis(plugin.getConfig().getString("asyncworldedit.watchDog.timeout", "5m"));
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        if (tStart == 0) {
            tStart = now;
        }
        Location chestLocation = LocationUtil.findChestLocation(islandLocation);
        if (chestLocation == null && now < timeout) {
            // Just run again
        } else {
            cancel();
            if (chestLocation == null && player != null && player.isOnline()) {
                player.sendMessage(I18nUtil.tr("\u00a7cWatchdog!\u00a79 Unable to locate a chest within {0}, bailing out.", TimeUtil.millisAsString(timeout - tStart)));
            }
            if (onCompletion != null) {
                onCompletion.setChestLocation(chestLocation);
                scheduler.sync(onCompletion);
            }
        }
    }
}
