package us.talabrek.ultimateskyblock.util;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.bukkit.entity.EntityType;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class EntityUtilTest extends BukkitServerMock {

    @BeforeClass
    public static void setUpClass() throws Exception {
        setupServerMock();
    }

    @Test
    public void testGetEntityDisplayName() {
        assertEquals("ArmorStand", EntityUtil.getEntityDisplayName(EntityType.ARMOR_STAND));
        assertEquals("Bat", EntityUtil.getEntityDisplayName(EntityType.BAT));
        assertEquals("CaveSpider", EntityUtil.getEntityDisplayName(EntityType.CAVE_SPIDER));
        assertEquals("WanderingTrader", EntityUtil.getEntityDisplayName(EntityType.WANDERING_TRADER));
    }

    @Test
    public void testGetEntityName() {
        Component name = EntityUtil.getEntityName(EntityType.COW);
        assertThat(name, instanceOf(TranslatableComponent.class));
        assertThat(((TranslatableComponent) name).key(), is(EntityType.COW.getTranslationKey()));
        assertThat(((TranslatableComponent) name).fallback(), is("Cow"));
    }
}
