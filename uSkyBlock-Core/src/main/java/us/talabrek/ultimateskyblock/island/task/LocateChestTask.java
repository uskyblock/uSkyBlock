package us.talabrek.ultimateskyblock.island.task;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import dk.lockfuglsang.minecraft.util.TimeUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.LocationUtil;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.time.Duration;
import java.time.Instant;

/**
 * A task that looks for a chest at an island location.
 */
public class LocateChestTask extends BukkitRunnable {
    private final Player player;
    private final Location islandLocation;
    private final GenerateTask onCompletion;
    private final Scheduler scheduler;
    private final Instant timeout;

    private Instant start;

    public LocateChestTask(uSkyBlock plugin, Player player, Location islandLocation, GenerateTask onCompletion) {
        this.scheduler = plugin.getScheduler();
        this.player = player;
        this.islandLocation = islandLocation;
        this.onCompletion = onCompletion;
        this.timeout = Instant.now().plus(TimeUtil.stringAsDuration(plugin.getConfig().getString("asyncworldedit.watchDog.timeout", "5m")));
    }

    @Override
    public void run() {
        if (start == null) {
            start = Instant.now();
        }
        Location chestLocation = LocationUtil.findChestLocation(islandLocation);
        if (chestLocation == null && Instant.now().isBefore(timeout)) {
            // Just run again
            // TODO: this is hacky, waiting for async generation to complete. Should ideally be launched once the generation has finished.
        } else {
            cancel();
            if (chestLocation == null && player != null && player.isOnline()) {
                player.sendMessage(I18nUtil.tr("\u00a7cWatchdog!\u00a79 Unable to locate a chest within {0}, bailing out.", TimeUtil.durationAsString(Duration.between(start, timeout))));
            }
            if (onCompletion != null) {
                onCompletion.setChestLocation(chestLocation);
                scheduler.sync(onCompletion);
            }
        }
    }
}
