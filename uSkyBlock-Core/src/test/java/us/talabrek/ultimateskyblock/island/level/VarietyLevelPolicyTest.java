package us.talabrek.ultimateskyblock.island.level;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VarietyLevelPolicyTest {
    private final VarietyLevelPolicy policy = new VarietyLevelPolicy(1);

    @Test
    void thousandOfOneTypeMatchesTenDistinctSingleBlocks() {
        double tenTypes = 10 * policy.levelFor(Material.STONE, 1);
        double thousandOfOneType = policy.levelFor(Material.STONE, 1000);
        assertEquals(10, tenTypes);
        assertEquals(tenTypes, thousandOfOneType, 1e-10);
        assertTrue(10 * policy.levelFor(Material.STONE, 100) > thousandOfOneType,
            "a varied build of the same total size must beat a single-material stack");
        assertEquals(policy.levelFor(Material.STONE, 1000), policy.levelFor(Material.DIAMOND_BLOCK, 1000),
            "material values must have no influence on the variety level");
    }

    @Test
    void additionalBlocksHaveRapidlyDecliningMarginalValue() {
        double first = policy.levelFor(Material.STONE, 1);
        double second = policy.levelFor(Material.STONE, 2) - first;
        double hundredth = policy.levelFor(Material.STONE, 100) - policy.levelFor(Material.STONE, 99);
        double thousandth = policy.levelFor(Material.STONE, 1000) - policy.levelFor(Material.STONE, 999);
        double beyondThousand = policy.levelFor(Material.STONE, 1001) - policy.levelFor(Material.STONE, 1000);
        assertEquals(1, first);
        assertTrue(second < first);
        assertTrue(hundredth < second / 10);
        assertTrue(thousandth < hundredth / 4);
        assertTrue(beyondThousand < thousandth / 3, "the tail must flatten after 1,000 blocks");
        assertEquals(10.5, policy.levelFor(Material.STONE, 2000), 1e-10);
        assertTrue(policy.levelFor(Material.STONE, 1_000_000) < 15,
            "even a million blocks of one material must not dominate late ranks");
    }

    @Test
    void nonFiniteOrInvalidScaleIsRejectedAndAirNeverCounts() {
        assertEquals(0, policy.levelFor(Material.AIR, 1000));
        assertEquals(0, policy.levelFor(Material.STONE, 0));
        assertThrows(IllegalArgumentException.class, () -> policy.levelFor(Material.STONE, -1));
        for (double invalid : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new VarietyLevelPolicy(invalid));
        }
        YamlConfiguration config = new YamlConfiguration();
        config.set("general.variety.unitsPerLevel", "many");
        assertThrows(IllegalArgumentException.class, () -> VarietyLevelPolicy.fromConfig(config));
    }

    @Test
    void scaleChangesLevelsWithoutChangingRelativeMaterialRewards() {
        VarietyLevelPolicy scaled = new VarietyLevelPolicy(2);
        assertEquals(policy.levelFor(Material.GLASS, 100) / 2, scaled.levelFor(Material.GLASS, 100));
    }
}
