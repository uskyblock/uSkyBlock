package us.talabrek.ultimateskyblock.platform.paper;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Paper's teleports: through {@code Entity#teleportAsync}, never Bukkit's teleport at once. */
class PaperTeleportsTest {
    private final Player player = mock(Player.class);
    private final Location to = new Location(mock(World.class), 10.5, 64.1, -5.5);

    @Test
    void aTeleportIsPapersAsyncTeleportAndItsFuture() {
        CompletableFuture<Boolean> arrival = new CompletableFuture<>();
        when(player.teleportAsync(to, TeleportCause.PLUGIN)).thenReturn(arrival);

        assertSame(arrival, new PaperTeleports().teleport(player, to));
        verify(player, never()).teleport(any(Location.class), any(TeleportCause.class));
    }

    @Test
    void aTeleportPaperCannotMakeFailsTheFuture() {
        IllegalArgumentException thrown = new IllegalArgumentException("World unloaded");
        when(player.teleportAsync(to, TeleportCause.PLUGIN)).thenThrow(thrown);

        CompletableFuture<Boolean> arrival = new PaperTeleports().teleport(player, to);

        assertSame(thrown, assertThrows(ExecutionException.class, arrival::get).getCause());
    }
}
