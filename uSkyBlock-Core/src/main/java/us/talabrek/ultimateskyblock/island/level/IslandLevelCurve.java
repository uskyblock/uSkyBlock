package us.talabrek.ultimateskyblock.island.level;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Converts total block points to island levels after block limits and diminishing returns.
 * The opening levels remain linear; above the anchor, the points required grow as level^exponent.
 * An exponent of one preserves the legacy calculation, including negative scores.
 */
public record IslandLevelCurve(double pointsPerLevel, double linearUntilLevel, double exponent) {
    public IslandLevelCurve {
        requirePositiveFinite(pointsPerLevel, "general.pointsPerLevel");
        requirePositiveFinite(linearUntilLevel, "general.levelProgression.linearUntilLevel");
        if (!Double.isFinite(exponent) || exponent < 1) {
            throw new IllegalArgumentException("general.levelProgression.exponent must be finite and at least 1");
        }
    }

    public static IslandLevelCurve fromConfig(ConfigurationSection config) {
        return new IslandLevelCurve(
            number(config, "general.pointsPerLevel", 1000),
            number(config, "general.levelProgression.linearUntilLevel", 100),
            number(config, "general.levelProgression.exponent", 1)
        );
    }

    public double levelForPoints(double points) {
        requireFinite(points, "Block points");
        double linearLevel = points / pointsPerLevel;
        double level = exponent == 1 || linearLevel <= linearUntilLevel
            ? linearLevel
            : linearUntilLevel * Math.pow(linearLevel / linearUntilLevel, 1 / exponent);
        requireFinite(level, "Calculated island level");
        return level;
    }

    /** Inverse conversion, useful for calibrating rank thresholds and explaining their cost. */
    public double pointsForLevel(double level) {
        requireFinite(level, "Island level");
        double linearLevel = exponent == 1 || level <= linearUntilLevel
            ? level
            : linearUntilLevel * Math.pow(level / linearUntilLevel, exponent);
        double points = linearLevel * pointsPerLevel;
        requireFinite(points, "Required block points");
        return points;
    }

    private static double number(ConfigurationSection config, String path, double fallback) {
        Object value = config.get(path);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new IllegalArgumentException(path + " must be a number");
    }

    private static void requirePositiveFinite(double value, String name) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException(name + " must be finite and greater than 0");
        }
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
