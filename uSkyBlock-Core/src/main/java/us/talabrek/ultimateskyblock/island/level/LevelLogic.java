package us.talabrek.ultimateskyblock.island.level;

import org.bukkit.Location;
import us.talabrek.ultimateskyblock.api.async.Callback;

public interface LevelLogic {
    /**
     * Starts an asynchronous score calculation for the island at the given location.
     *
     * <p>{@link Callback} carries no failure channel, so the return value is the only way a caller
     * can tell that its callback will never run. Implementations must log the reason before
     * returning {@code false}; a caller that ignores it leaves whatever it scheduled in the
     * callback permanently pending.</p>
     *
     * @param l        island location to score
     * @param callback invoked on success only
     * @return {@code true} if the callback will be invoked, {@code false} if the calculation could
     *         not be started (already logged)
     */
    boolean calculateScoreAsync(Location l, Callback<IslandScore> callback);
}
