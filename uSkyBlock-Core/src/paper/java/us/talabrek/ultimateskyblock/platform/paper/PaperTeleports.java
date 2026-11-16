package us.talabrek.ultimateskyblock.platform.paper;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.platform.Teleports;

import java.util.concurrent.CompletableFuture;

/** Paper: {@code Entity#teleportAsync}, which loads the target's chunks off the server thread first. */
public final class PaperTeleports implements Teleports {
    @Override
    public @NotNull CompletableFuture<Boolean> teleport(@NotNull Entity entity, @NotNull Location to) {
        try {
            return entity.teleportAsync(to, TeleportCause.PLUGIN);
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
