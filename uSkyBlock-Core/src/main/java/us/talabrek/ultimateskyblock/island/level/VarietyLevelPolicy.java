package us.talabrek.ultimateskyblock.island.level;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * An island level based on distinct block materials, not their configured block values.
 * A thousand blocks of one type earn as much as ten distinct single blocks. Each additional
 * block earns less than the previous one, with a flatter tail after one thousand blocks.
 * Air and the materials on the level blacklist never count.
 */
public record VarietyLevelPolicy(double unitsPerLevel, Set<Material> ignored) {
    /** Count from which the flat tail applies; below it the level grows with the cube root. */
    public static final int TAIL_START = 1000;
    private static final double TAIL_UNITS_PER_DOUBLING = 0.5;
    private static final double LOG_2 = Math.log(2);

    public VarietyLevelPolicy {
        if (!Double.isFinite(unitsPerLevel) || unitsPerLevel <= 0) {
            throw new IllegalArgumentException("general.variety.unitsPerLevel must be finite and positive");
        }
        ignored = ignored.isEmpty()
            ? Collections.emptySet()
            : Collections.unmodifiableSet(EnumSet.copyOf(ignored));
    }

    public VarietyLevelPolicy(double unitsPerLevel) {
        this(unitsPerLevel, Set.of());
    }

    public static VarietyLevelPolicy fromConfig(ConfigurationSection config, Set<Material> ignored) {
        Object value = config.get("general.variety.unitsPerLevel");
        if (value == null) {
            return new VarietyLevelPolicy(1, ignored);
        }
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("general.variety.unitsPerLevel must be a number");
        }
        return new VarietyLevelPolicy(number.doubleValue(), ignored);
    }

    /** Levels gained by the first block of a new type: one unit, scaled. */
    public double levelPerNewType() {
        return 1 / unitsPerLevel;
    }

    /** Whether blocks of this material contribute to the level at all. */
    public boolean counts(Material material) {
        // Explicit air check rather than Material#isAir(): the latter resolves the block registry, which
        // only a running server provides. The bundled blacklist lists the air types as well.
        return material != Material.AIR && material != Material.CAVE_AIR && material != Material.VOID_AIR
            && !ignored.contains(material);
    }

    /** Whether a count of one material is in the flat tail, where every doubling adds only half a unit. */
    public boolean isInTail(int count) {
        return count > TAIL_START;
    }

    public double levelFor(Material material, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Block count must not be negative");
        }
        if (count == 0 || !counts(material)) {
            return 0;
        }
        double units = isInTail(count)
            ? 10 + TAIL_UNITS_PER_DOUBLING * Math.log(count / (double) TAIL_START) / LOG_2
            : Math.cbrt(count);
        return units / unitsPerLevel;
    }
}
