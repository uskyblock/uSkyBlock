package us.talabrek.ultimateskyblock.island;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Cow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.LivingEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.island.LimitLogic.CreatureType;
import us.talabrek.ultimateskyblock.world.WorldManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class LimitLogicTest {
    private final World world = mock(World.class);
    private final IslandInfo islandInfo = mock(IslandInfo.class);
    private final List<LivingEntity> creatures = new ArrayList<>();
    private LimitLogic limitLogic;
    private MockedStatic<WorldGuardHandler> worldGuard;

    @BeforeEach
    public void setUp() {
        WorldManager worldManager = mock(WorldManager.class);
        when(worldManager.getWorld()).thenReturn(world);
        when(islandInfo.getIslandLocation()).thenReturn(new Location(world, 0, 100, 0));
        ProtectedRegion region = mock(ProtectedRegion.class);
        worldGuard = mockStatic(WorldGuardHandler.class);
        worldGuard.when(() -> WorldGuardHandler.getIslandRegionAt(any())).thenReturn(region);
        worldGuard.when(() -> WorldGuardHandler.getCreaturesInRegion(world, region)).thenAnswer(a -> new ArrayList<>(creatures));
        limitLogic = new LimitLogic(worldManager, mock(BlockLimitLogic.class));
    }

    @AfterEach
    public void tearDown() {
        worldGuard.close();
    }

    @Test
    public void happyGhastsCountedSeparatelyFromAnimals() {
        creatures.add(creature(HappyGhast.class, EntityType.HAPPY_GHAST));
        creatures.add(creature(Cow.class, EntityType.COW));

        Map<CreatureType, Integer> count = limitLogic.getCreatureCount(islandInfo);

        assertEquals(1, count.get(CreatureType.HAPPY_GHAST));
        assertEquals(1, count.get(CreatureType.ANIMAL));
        assertEquals(CreatureType.HAPPY_GHAST, limitLogic.getCreatureType(EntityType.HAPPY_GHAST));
    }

    @Test
    public void happyGhastsLimitedByTheirOwnCap() {
        when(islandInfo.getMaxAnimals()).thenReturn(64);
        when(islandInfo.getMaxHappyGhasts()).thenReturn(2);
        creatures.add(creature(HappyGhast.class, EntityType.HAPPY_GHAST));

        assertTrue(limitLogic.canSpawn(EntityType.HAPPY_GHAST, islandInfo));

        creatures.add(creature(HappyGhast.class, EntityType.HAPPY_GHAST));

        assertFalse(limitLogic.canSpawn(EntityType.HAPPY_GHAST, islandInfo));
        assertTrue(limitLogic.canSpawn(EntityType.COW, islandInfo));
    }

    private static LivingEntity creature(Class<? extends LivingEntity> type, EntityType entityType) {
        LivingEntity creature = mock(type);
        when(creature.getType()).thenReturn(entityType);
        return creature;
    }
}
