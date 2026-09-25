package us.talabrek.ultimateskyblock.island.level;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

/**
 * An island level based on distinct block materials, not their configured block values.
 * A thousand blocks of one type earn as much as ten distinct single blocks. Each additional
 * block earns less than the previous one, with a flatter tail after one thousand blocks.
 */
public record VarietyLevelPolicy(double unitsPerLevel) {
    private static final int TAIL_START = 1000;
    private static final double TAIL_UNITS_PER_DOUBLING = 0.5;
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
        double units = count <= TAIL_START
            ? Math.cbrt(count)
            : 10 + TAIL_UNITS_PER_DOUBLING * Math.log(count / (double) TAIL_START) / LOG_2;
        return units / unitsPerLevel;
    }
}
