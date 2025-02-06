package us.talabrek.ultimateskyblock.api.event;

import com.google.inject.Inject;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.util.Scheduler;

public class EventLogic {
    private final Scheduler scheduler;

    @Inject
    public EventLogic(@NotNull Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * Fires a new async {@link IslandLeaderChangedEvent}.
     *
     * @param islandInfo         {@link IslandInfo} for the island in the scope of this event.
     * @param originalLeaderInfo {@link PlayerInfo} for the original island leader.
     * @param newLeaderInfo      {@link PlayerInfo} for the new island leader.
     */
    public void fireIslandLeaderChangedEvent(us.talabrek.ultimateskyblock.api.IslandInfo islandInfo,
                                             us.talabrek.ultimateskyblock.api.PlayerInfo originalLeaderInfo,
                                             us.talabrek.ultimateskyblock.api.PlayerInfo newLeaderInfo) {
        scheduler.async(() -> Bukkit.getPluginManager().callEvent(new IslandLeaderChangedEvent(islandInfo, originalLeaderInfo, newLeaderInfo)));
    }

    /**
     * Fires a new async {@link MemberJoinedEvent}.
     *
     * @param islandInfo {@link IslandInfo} for the island that the member joined.
     * @param playerInfo {@link PlayerInfo} for the joined member.
     */
    public void fireMemberJoinedEvent(us.talabrek.ultimateskyblock.island.IslandInfo islandInfo, us.talabrek.ultimateskyblock.player.PlayerInfo playerInfo) {
        scheduler.async(() -> Bukkit.getPluginManager().callEvent(new MemberJoinedEvent(islandInfo, playerInfo)));
    }

    /**
     * Fires a new async {@link MemberLeftEvent}.
     *
     * @param islandInfo {@link IslandInfo} for the island that the member left.
     * @param member     {@link PlayerInfo} for the left member.
     */
    public void fireMemberLeftEvent(IslandInfo islandInfo, PlayerInfo member) {
        scheduler.async(() -> Bukkit.getPluginManager().callEvent(new MemberLeftEvent(islandInfo, member)));
    }

    public void shutdown() {
        // Placeholder for now.
    }
}
