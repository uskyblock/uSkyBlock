package us.talabrek.ultimateskyblock.handler;

import org.bukkit.entity.Player;
import org.junit.Test;
import us.talabrek.ultimateskyblock.test.MutableClock;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CooldownHandlerTest {

    @Test
    public void testCommandCooldown() {
        Instant fixedInstant = Instant.parse("2025-02-10T12:00:00Z");
        MutableClock testClock = new MutableClock(fixedInstant);
        var cooldownHandler = new CooldownHandler(testClock, mock());
        UUID testId = new UUID(1, 2);
        Player mockPlayer = mock();
        when(mockPlayer.getUniqueId()).thenReturn(testId);

        assertEquals(Duration.ZERO, cooldownHandler.getCooldown(mockPlayer, "test"));

        cooldownHandler.resetCooldown(mockPlayer, "test", Duration.ofSeconds(1));
        assertEquals(Duration.ofSeconds(1), cooldownHandler.getCooldown(mockPlayer, "test"));

        testClock.advance(Duration.ofSeconds(1));
        assertEquals(Duration.ZERO, cooldownHandler.getCooldown(mockPlayer, "test"));

        testClock.advance(Duration.ofSeconds(1));
        assertEquals(Duration.ZERO, cooldownHandler.getCooldown(mockPlayer, "test"));
    }
}
