package us.talabrek.ultimateskyblock.island.level;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VarietyLevelPolicyTest {
    private final VarietyLevelPolicy policy = new VarietyLevelPolicy(1);

    @Test
    void everyNewTypeIsWorthMoreThanAnotherStackOfOneType() {
        double tenTypes = 10 * policy.levelFor(Material.STONE, 1);
        double thousandOfOneType = policy.levelFor(Material.STONE, 1000);
        assertEquals(10, tenTypes);
        assertTrue(tenTypes > thousandOfOneType);
        assertEquals(policy.levelFor(Material.STONE, 1000), policy.levelFor(Material.DIAMOND_BLOCK, 1000),
            "material values must have no influence on the variety level");
    }

    @Test
    void additionalBlocksHaveRapidlyDecliningMarginalValue() {
        double first = policy.levelFor(Material.STONE, 1);
        double second = policy.levelFor(Material.STONE, 2) - first;
        double hundredth = policy.levelFor(Material.STONE, 100) - policy.levelFor(Material.STONE, 99);
        double thousandth = policy.levelFor(Material.STONE, 1000) - policy.levelFor(Material.STONE, 999);
        assertEquals(1, first);
        assertTrue(second < first);
        assertTrue(hundredth < second / 20);
        assertTrue(thousandth < hundredth / 8);
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
