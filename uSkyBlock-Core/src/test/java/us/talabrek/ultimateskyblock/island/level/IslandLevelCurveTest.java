package us.talabrek.ultimateskyblock.island.level;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IslandLevelCurveTest {
    @Test
    void absentSettingsPreserveLegacyLevelsIncludingNegativeScores() {
        IslandLevelCurve curve = IslandLevelCurve.fromConfig(new YamlConfiguration());
        assertEquals(-2.5, curve.levelForPoints(-2500));
        assertEquals(0, curve.levelForPoints(0));
        assertEquals(20, curve.levelForPoints(20_000));
        assertEquals(1500, curve.levelForPoints(1_500_000));
    }

    @Test
    void existingCustomPointsPerLevelIsRespected() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("general.pointsPerLevel", 250);
        assertEquals(40, IslandLevelCurve.fromConfig(config).levelForPoints(10_000));
    }

    @Test
    void quadraticCurvePreservesOpeningAndHasKnownThresholdCosts() {
        IslandLevelCurve curve = new IslandLevelCurve(1000, 100, 2);
        assertEquals(-20, curve.levelForPoints(-20_000));
        assertEquals(20, curve.levelForPoints(20_000));
        assertEquals(100, curve.levelForPoints(100_000));
        assertEquals(200, curve.levelForPoints(400_000));
        assertEquals(400_000, curve.pointsForLevel(200));
        assertEquals(1_600_000, curve.pointsForLevel(400));
        assertEquals(3_600_000, curve.pointsForLevel(600));
    }

    @Test
    void curveIsContinuousAtTheAnchorAndPreservesOrdering() {
        IslandLevelCurve curve = new IslandLevelCurve(1000, 100, 1.5);
        assertEquals(100, curve.levelForPoints(100_000 - 0.001), 0.00001);
        assertEquals(100, curve.levelForPoints(100_000 + 0.001), 0.00001);
        double previousLevel = -1;
        for (double points : new double[]{0, 1, 20_000, 100_000, 100_001, 400_000, 1_600_000}) {
            double level = curve.levelForPoints(points);
            assertTrue(level > previousLevel);
            assertEquals(points, curve.pointsForLevel(level), Math.max(1e-9, points * 1e-12));
            previousLevel = level;
        }
        double earlyCost = curve.pointsForLevel(200) - curve.pointsForLevel(100);
        double laterCost = curve.pointsForLevel(300) - curve.pointsForLevel(200);
        assertTrue(laterCost > earlyCost, "the same level gain must cost more later");
    }

    @Test
    void rejectsInvalidConfigurationInsteadOfProducingBrokenRankings() {
        for (double invalid : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new IslandLevelCurve(invalid, 100, 1));
            assertThrows(IllegalArgumentException.class, () -> new IslandLevelCurve(1000, invalid, 1));
        }
        for (double invalid : new double[]{0.5, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new IslandLevelCurve(1000, 100, invalid));
        }
        YamlConfiguration config = new YamlConfiguration();
        config.set("general.levelProgression.exponent", "quadratic");
        assertTrue(assertThrows(IllegalArgumentException.class,
            () -> IslandLevelCurve.fromConfig(config)).getMessage().contains("general.levelProgression.exponent"));
    }

    @Test
    void rejectsNonFiniteInputsAndOverflow() {
        IslandLevelCurve curve = new IslandLevelCurve(1000, 100, 2);
        assertThrows(IllegalArgumentException.class, () -> curve.levelForPoints(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> curve.pointsForLevel(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> curve.pointsForLevel(Double.MAX_VALUE));
    }
}
