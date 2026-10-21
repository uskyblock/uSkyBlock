package us.talabrek.ultimateskyblock.event;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * The bounce of {@link WorldGuardEvents}. Which players it bounces is decided through WorldGuard (compileOnly, not on
 * the test classpath), so that part is left to the live-server harness.
 */
public class WorldGuardEventsTest {
    @Test
    public void aPlayerOnTheNetherIslandIsBouncedAwayFromTheIslandsCentre() {
        Location island = new Location(mock(World.class), 128, 150, 0);
        Location onTheNetherIsland = new Location(mock(World.class), 131, 75, 4);

        Vector bounce = WorldGuardEvents.bounce(onTheNetherIsland, island);

        assertEquals(0.9, bounce.getX(), 1e-9);
        assertEquals(0, bounce.getY(), 1e-9);
        assertEquals(1.2, bounce.getZ(), 1e-9);
    }

    @Test
    public void aPlayerAtTheCentresXAndZIsPushedBackFromWhereTheyFace() {
        Location island = new Location(mock(World.class), 128, 150, 0);
        // Facing south, +z.
        Location aboveTheCentre = new Location(mock(World.class), 128, 75, 0, 0, 90);

        Vector bounce = WorldGuardEvents.bounce(aboveTheCentre, island);

        assertEquals(0, bounce.getX(), 1e-9);
        assertEquals(0, bounce.getY(), 1e-9);
        assertEquals(-1.5, bounce.getZ(), 1e-9);
    }

    @Test
    public void aPlayerAtTheCentreFacingWestIsPushedEast() {
        Location island = new Location(mock(World.class), 128, 150, 0);
        Location atTheCentre = new Location(mock(World.class), 128, 150, 0, 90, 0);

        Vector bounce = WorldGuardEvents.bounce(atTheCentre, island);

        assertEquals(1.5, bounce.getX(), 1e-9);
        assertEquals(0, bounce.getZ(), 1e-9);
    }
}
