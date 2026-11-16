package us.talabrek.ultimateskyblock.platform;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Teleports with the cause {@code PLUGIN}. Paper loads the chunks at the target off the server thread first and then
 * teleports on it, later; Spigot teleports at once.
 */
public interface Teleports {
    /**
     * Never throws: a teleport the server cannot make, as into an unloaded world or to a location that is not finite,
     * fails the future.
     *
     * @return completes on the server thread with whether the entity arrived: false where the teleport was refused, as
     * by a cancelled event
     */
    @NotNull CompletableFuture<Boolean> teleport(@NotNull Entity entity, @NotNull Location to);
}
