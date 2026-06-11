package us.talabrek.ultimateskyblock.world;

import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WorldManagerTest {

    @Test
    public void hasExpectedGeneratorIsFalseWhenNoGeneratorAttached() {
        World world = mock(World.class);
        when(world.getGenerator()).thenReturn(null);

        assertFalse(WorldManager.hasExpectedGenerator(world, new SkyBlockChunkGenerator()));
    }

    @Test
    public void hasExpectedGeneratorIsTrueForSameGeneratorClass() {
        World world = mock(World.class);
        when(world.getGenerator()).thenReturn(new SkyBlockChunkGenerator());

        assertTrue(WorldManager.hasExpectedGenerator(world, new SkyBlockChunkGenerator()));
    }

    @Test
    public void hasExpectedGeneratorIsFalseForDifferentGeneratorClass() {
        World world = mock(World.class);
        when(world.getGenerator()).thenReturn(new SkyBlockNetherChunkGenerator());

        assertFalse(WorldManager.hasExpectedGenerator(world, new SkyBlockChunkGenerator()));
    }

    @Test
    public void wrongGeneratorWarningContainsActionableInstructions() {
        List<String> lines = WorldManager.wrongGeneratorWarning("skyworld");

        String joined = String.join("\n", lines);
        assertThat(joined, containsString("skyworld"));
        assertThat(joined, containsString("worlds:"));
        assertThat(joined, containsString("generator: uSkyBlock"));
        assertThat(joined, containsString("/usb chunk regen"));
    }
}
