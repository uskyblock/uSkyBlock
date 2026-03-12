package us.talabrek.ultimateskyblock.util;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BlockUtilTest {

    @Disabled
    @Test
    public void testIsBreathable() throws Exception {
        Block fakeBlock = Mockito.mock(Block.class);

        Mockito.when(fakeBlock.getType()).thenReturn(Material.DIRT);
        assertFalse(BlockUtil.isBreathable(fakeBlock));

        Mockito.when(fakeBlock.getType()).thenReturn(Material.SHORT_GRASS);
        assertTrue(BlockUtil.isBreathable(fakeBlock));

        Mockito.when(fakeBlock.getType()).thenReturn(Material.WATER);
        assertFalse(BlockUtil.isBreathable(fakeBlock));
    }

    @Test
    public void testIsFluidMaterial() throws Exception {
        assertFalse(BlockUtil.isFluid(Material.DIAMOND_BLOCK));
        assertFalse(BlockUtil.isFluid(Material.LAVA_BUCKET));

        assertTrue(BlockUtil.isFluid(Material.WATER));
        assertTrue(BlockUtil.isFluid(Material.LAVA));
    }

    @Test
    public void testIsFluidBlock() throws Exception {
        Block fakeBlock = Mockito.mock(Block.class);

        Mockito.when(fakeBlock.getType()).thenReturn(Material.WATER);
        assertTrue(BlockUtil.isFluid(fakeBlock));

        Mockito.when(fakeBlock.getType()).thenReturn(Material.AIR);
        assertFalse(BlockUtil.isFluid(fakeBlock));
    }
}
