package us.talabrek.ultimateskyblock.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired on the main thread when a challenge completion unlocks a challenge rank for an island.
 *
 * <p>Only completion-driven transitions are detected: a rank that becomes reachable purely
 * through island-level growth does not fire this event until the next challenge completion.</p>
 *
 * @since 4.0
 */
public class ChallengeRankUnlockedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final String islandName;
    private final String rankId;

    public ChallengeRankUnlockedEvent(@NotNull String islandName, @NotNull String rankId) {
        this.islandName = islandName;
        this.rankId = rankId;
    }

    public @NotNull String getIslandName() {
        return islandName;
    }

    public @NotNull String getRankId() {
        return rankId;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
