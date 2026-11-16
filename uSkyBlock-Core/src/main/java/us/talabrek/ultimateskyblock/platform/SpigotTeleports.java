package us.talabrek.ultimateskyblock.platform;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/** Spigot: at once, loading the target's chunks on the server thread. */
public final class SpigotTeleports implements Teleports {
    @Override
    public @NotNull CompletableFuture<Boolean> teleport(@NotNull Entity entity, @NotNull Location to) {
        try {
            return CompletableFuture.completedFuture(entity.teleport(to, TeleportCause.PLUGIN));
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
