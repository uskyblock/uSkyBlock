package us.talabrek.ultimateskyblock.event;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.api.event.CreateIslandEvent;
import us.talabrek.ultimateskyblock.api.event.IslandInfoEvent;
import us.talabrek.ultimateskyblock.api.event.MemberJoinedEvent;
import us.talabrek.ultimateskyblock.api.event.MemberLeftEvent;
import us.talabrek.ultimateskyblock.api.event.RestartIslandEvent;
import us.talabrek.ultimateskyblock.api.event.uSkyBlockScoreChangedEvent;
import us.talabrek.ultimateskyblock.island.level.IslandScore;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.LocationUtil;

/**
 * Main event-handler for internal uSkyBlock events
 */
@Singleton
public class InternalEvents implements Listener {
    private final uSkyBlock plugin;
    private final RuntimeConfigs runtimeConfigs;

    @Inject
    public InternalEvents(@NotNull uSkyBlock plugin, @NotNull RuntimeConfigs runtimeConfigs) {
        this.plugin = plugin;
        this.runtimeConfigs = runtimeConfigs;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRestart(RestartIslandEvent e) {
        plugin.restartPlayerIsland(e.getPlayer(), e.getIslandLocation(), e.getSchematic());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreate(CreateIslandEvent e) {
        plugin.createIsland(e.getPlayer(), e.getSchematic());
    }

    @EventHandler
    public void onMemberJoin(MemberJoinedEvent e) {
        PlayerInfo playerInfo = (PlayerInfo) e.getPlayerInfo();
        playerInfo.execCommands(runtimeConfigs.current().party().joinCommands());
    }

    @EventHandler
    public void onMemberLeft(MemberLeftEvent e) {
        PlayerInfo playerInfo = (PlayerInfo) e.getPlayerInfo();
        playerInfo.execCommands(runtimeConfigs.current().party().leaveCommands());
    }

    @EventHandler
    public void onScoreChanged(uSkyBlockScoreChangedEvent e) {
        plugin.getBlockLimitLogic().updateBlockCount(e.getIslandLocation(), (IslandScore) e.getScore());
    }

    @EventHandler
    public void onInfoEvent(IslandInfoEvent e) {
        String islandName = LocationUtil.getIslandName(e.getIslandLocation());
        if (!plugin.calculateScoreAsync(e.getPlayer(), islandName, e.getCallback())) {
            // IslandInfoEvent exposes no failure channel, so a consumer waiting on the callback
            // cannot learn this; the log is the only signal. Reuse the already-computed name rather
            // than re-deriving from the Location, whose World is a WeakReference that throws once
            // the world is unloaded.
            plugin.getLogger().warning(() -> "IslandInfoEvent callback will not run for island '"
                + islandName + "'; its level could not be calculated.");
        }
    }
}
