package us.talabrek.ultimateskyblock.event;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
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

    @Inject
    public InternalEvents(@NotNull uSkyBlock plugin) {
        this.plugin = plugin;
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
        playerInfo.execCommands(plugin.getConfig().getStringList("options.party.join-commands"));
    }

    @EventHandler
    public void onMemberLeft(MemberLeftEvent e) {
        PlayerInfo playerInfo = (PlayerInfo) e.getPlayerInfo();
        playerInfo.execCommands(plugin.getConfig().getStringList("options.party.leave-commands"));
    }

    @EventHandler
    public void onScoreChanged(uSkyBlockScoreChangedEvent e) {
        plugin.getBlockLimitLogic().updateBlockCount(e.getIslandLocation(), (IslandScore) e.getScore());
    }

    @EventHandler
    public void onInfoEvent(IslandInfoEvent e) {
        plugin.calculateScoreAsync(e.getPlayer(), LocationUtil.getIslandName(e.getIslandLocation()), e.getCallback());
    }
}
