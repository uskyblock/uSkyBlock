package us.talabrek.ultimateskyblock.island.task;

import org.bukkit.scheduler.BukkitRunnable;
import us.talabrek.ultimateskyblock.api.async.Callback;
import us.talabrek.ultimateskyblock.api.event.uSkyBlockEvent;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RecalculateTopTen extends BukkitRunnable {
    private final Queue<String> locations;
    private final uSkyBlock plugin;
    private final Scheduler scheduler;

    public RecalculateTopTen(uSkyBlock plugin, Scheduler scheduler, Collection<String> locations) {
        this.locations = new ConcurrentLinkedQueue<>(locations);
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    @Override
    public void run() {
        String islandName = locations.poll();
        if (islandName != null) {
            boolean started = plugin.calculateScoreAsync(null, islandName, new Callback<>() {
                @Override
                public void run() {
                    // We use the deprecated on purpose (the other would fail).
                    scheduler.async(RecalculateTopTen.this);
                }
            });
            if (!started) {
                // The queue is only advanced from inside the callback, so a single unscoreable
                // island would otherwise stall the whole recalculation for good. Skip it instead.
                plugin.getLogger().warning(() -> "Skipping island '" + islandName
                    + "' in top-ten recalculation; its level could not be calculated.");
                scheduler.async(RecalculateTopTen.this);
            }
        } else {
            plugin.fireAsyncEvent(new uSkyBlockEvent(null, uSkyBlock.getAPI(), uSkyBlockEvent.Cause.RANK_UPDATED));
        }
    }
}
