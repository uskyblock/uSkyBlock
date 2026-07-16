package us.talabrek.ultimateskyblock.island;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Golem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WaterMob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.api.IslandInfo;
import us.talabrek.ultimateskyblock.island.LimitLogic.CreatureType;
import us.talabrek.ultimateskyblock.world.WorldManager;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Focused unit tests for {@link LimitLogic}'s creature classification and max-limit routing.
 *
 * Note: {@code canSpawn}/{@code getCreatureCount} are welded to {@code WorldGuardHandler}'s
 * static region scanning (which needs the {@code uSkyBlock} god-object and the WorldGuard API,
 * a compile-only dependency absent from the test classpath), so they are not exercised here.
 * The private {@code getMax} routing is covered WorldGuard-free through {@code getCreatureMax}.
 */
public class LimitLogicTest {

    private LimitLogic limitLogic;

    @BeforeEach
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        BukkitServerMock.setupServerMock();
        limitLogic = new LimitLogic(mock(WorldManager.class), mock(BlockLimitLogic.class));
    }

    @Test
    public void getCreatureType_livingEntity_classifiesByInterface() {
        assertEquals(CreatureType.MONSTER, limitLogic.getCreatureType(mock(Monster.class)));
        // WaterMob and Ghast do NOT extend Monster; they are explicitly folded into MONSTER.
        assertEquals(CreatureType.MONSTER, limitLogic.getCreatureType(mock(WaterMob.class)));
        assertEquals(CreatureType.MONSTER, limitLogic.getCreatureType(mock(Ghast.class)));
        assertEquals(CreatureType.ANIMAL, limitLogic.getCreatureType(mock(Animals.class)));
        assertEquals(CreatureType.VILLAGER, limitLogic.getCreatureType(mock(Villager.class)));
        // Plain LivingEntity matches none of the categories.
        assertEquals(CreatureType.UNKNOWN, limitLogic.getCreatureType(mock(LivingEntity.class)));
    }

    @Test
    public void getCreatureType_livingEntity_golemBranchSplitsCopper() {
        Golem ironGolem = mock(Golem.class);
        when(ironGolem.getType()).thenReturn(EntityType.IRON_GOLEM);
        assertEquals(CreatureType.GOLEM, limitLogic.getCreatureType(ironGolem));

        Golem copperGolem = mock(Golem.class);
        when(copperGolem.getType()).thenReturn(EntityType.COPPER_GOLEM);
        assertEquals(CreatureType.COPPER_GOLEM, limitLogic.getCreatureType(copperGolem));
    }

    @Test
    public void getCreatureType_entityType_classifiesByEntityClass() {
        assertEquals(CreatureType.MONSTER, limitLogic.getCreatureType(EntityType.ZOMBIE));
        // Slime, Ghast and Squid (a WaterMob) are not Monster subtypes but count as MONSTER.
        assertEquals(CreatureType.MONSTER, limitLogic.getCreatureType(EntityType.SLIME));
        assertEquals(CreatureType.MONSTER, limitLogic.getCreatureType(EntityType.GHAST));
        assertEquals(CreatureType.MONSTER, limitLogic.getCreatureType(EntityType.SQUID));
        assertEquals(CreatureType.ANIMAL, limitLogic.getCreatureType(EntityType.COW));
        assertEquals(CreatureType.VILLAGER, limitLogic.getCreatureType(EntityType.VILLAGER));
        assertEquals(CreatureType.GOLEM, limitLogic.getCreatureType(EntityType.IRON_GOLEM));
        // Shulker extends Golem (not Monster), so it classifies as GOLEM.
        assertEquals(CreatureType.GOLEM, limitLogic.getCreatureType(EntityType.SHULKER));
        assertEquals(CreatureType.COPPER_GOLEM, limitLogic.getCreatureType(EntityType.COPPER_GOLEM));
        // Armor stand is a LivingEntity but none of the tracked categories.
        assertEquals(CreatureType.UNKNOWN, limitLogic.getCreatureType(EntityType.ARMOR_STAND));
    }

    @Test
    public void getCreatureMax_routesEachTypeToMatchingIslandLimit() {
        IslandInfo islandInfo = mock(IslandInfo.class);
        when(islandInfo.getMaxAnimals()).thenReturn(3);
        when(islandInfo.getMaxMonsters()).thenReturn(7);
        when(islandInfo.getMaxVillagers()).thenReturn(2);
        when(islandInfo.getMaxGolems()).thenReturn(5);
        when(islandInfo.getMaxCopperGolems()).thenReturn(1);

        Map<CreatureType, Integer> max = limitLogic.getCreatureMax(islandInfo);

        assertEquals(3, max.get(CreatureType.ANIMAL).intValue());
        assertEquals(7, max.get(CreatureType.MONSTER).intValue());
        assertEquals(2, max.get(CreatureType.VILLAGER).intValue());
        assertEquals(5, max.get(CreatureType.GOLEM).intValue());
        assertEquals(1, max.get(CreatureType.COPPER_GOLEM).intValue());
        // UNKNOWN has no dedicated limit and falls through to the unlimited default.
        assertEquals(Integer.MAX_VALUE, max.get(CreatureType.UNKNOWN).intValue());
    }
}
