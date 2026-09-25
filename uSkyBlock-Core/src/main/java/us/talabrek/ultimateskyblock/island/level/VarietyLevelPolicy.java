package us.talabrek.ultimateskyblock.island.level;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

/**
 * An island level based on distinct block materials, not their configured block values.
 * Each new type earns one unit; repetitions earn a small logarithmic bonus. The gain from
 * the thousandth block of a type is much smaller than the gain from discovering another type.
 */
public record VarietyLevelPolicy(double unitsPerLevel) {
    private static final double REPEAT_UNITS = 0.25;
    private static final double LOG_2 = Math.log(2);

    public VarietyLevelPolicy {
        if (!Double.isFinite(unitsPerLevel) || unitsPerLevel <= 0) {
            throw new IllegalArgumentException("general.variety.unitsPerLevel must be finite and positive");
        }
    }

    public static VarietyLevelPolicy fromConfig(ConfigurationSection config) {
        Object value = config.get("general.variety.unitsPerLevel");
        if (value == null) {
            return new VarietyLevelPolicy(1);
        }
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("general.variety.unitsPerLevel must be a number");
        }
        return new VarietyLevelPolicy(number.doubleValue());
    }

    public double levelFor(Material material, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Block count must not be negative");
        }
        if (count == 0 || material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR) {
            return 0;
        }
        return (1 + REPEAT_UNITS * Math.log(count) / LOG_2) / unitsPerLevel;
    }
}
