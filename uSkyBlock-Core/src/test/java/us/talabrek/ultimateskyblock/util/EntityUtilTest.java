package us.talabrek.ultimateskyblock.util;

import org.bukkit.entity.EntityType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EntityUtilTest {

    @Test
    public void testGetEntityDisplayName() {
        assertEquals("ArmorStand", EntityUtil.getEntityDisplayName(EntityType.ARMOR_STAND));
        assertEquals("Bat", EntityUtil.getEntityDisplayName(EntityType.BAT));
        assertEquals("CaveSpider", EntityUtil.getEntityDisplayName(EntityType.CAVE_SPIDER));
        assertEquals("WanderingTrader", EntityUtil.getEntityDisplayName(EntityType.WANDERING_TRADER));
    }
}
