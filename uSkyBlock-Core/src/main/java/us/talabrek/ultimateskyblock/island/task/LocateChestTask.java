package us.talabrek.ultimateskyblock.island.task;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import dk.lockfuglsang.minecraft.util.TimeUtil;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.LocationUtil;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.time.Duration;
import java.time.Instant;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;

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

    public LocateChestTask(uSkyBlock plugin, RuntimeConfigs runtimeConfigs, Player player, Location islandLocation, GenerateTask onCompletion) {
        this.scheduler = plugin.getScheduler();
        this.player = player;
        this.islandLocation = islandLocation;
        this.onCompletion = onCompletion;
        this.timeout = Instant.now().plus(runtimeConfigs.current().asyncWorldEdit().timeout());
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
                sendErrorTr(player, "Watchdog!<primary> Unable to locate a chest within <timeout>, bailing out.",
                    unparsed("timeout", TimeUtil.durationAsString(Duration.between(start, timeout))));
            }
            if (onCompletion != null) {
                onCompletion.setChestLocation(chestLocation);
                scheduler.sync(onCompletion);
            }
        }
    }
}
