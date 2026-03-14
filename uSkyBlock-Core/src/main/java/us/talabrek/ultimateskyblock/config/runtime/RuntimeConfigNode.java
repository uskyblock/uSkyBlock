package us.talabrek.ultimateskyblock.config.runtime;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.config.ConfigDuration;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

final class RuntimeConfigNode {
    private final Logger logger;
    private final String path;
    private final ConfigurationSection configured;
    private final ConfigurationSection defaults;

    private RuntimeConfigNode(@NotNull Logger logger, @NotNull String path, @Nullable ConfigurationSection configured, @Nullable ConfigurationSection defaults) {
        this.logger = logger;
        this.path = path;
        this.configured = configured;
        this.defaults = defaults;
    }

    @NotNull
    static RuntimeConfigNode root(@NotNull FileConfiguration configured, @NotNull YamlConfiguration defaults, @NotNull Logger logger) {
        return new RuntimeConfigNode(logger, "", configured, defaults);
    }

    boolean exists() {
        return configured != null || defaults != null;
    }

    @NotNull
    RuntimeConfigNode child(@NotNull String relativePath) {
        String childPath = path.isEmpty() ? relativePath : path + "." + relativePath;
        Object configuredValue = configuredValue(relativePath);
        if (configuredValue != null && !(configuredValue instanceof ConfigurationSection)) {
            warnInvalidType(childPath, configuredValue, "section", defaults != null ? defaults.getConfigurationSection(relativePath) : null);
        }
        return new RuntimeConfigNode(
            logger,
            childPath,
            configured != null ? configured.getConfigurationSection(relativePath) : null,
            defaults != null ? defaults.getConfigurationSection(relativePath) : null
        );
    }

    boolean isSection(@NotNull String key) {
        return (configured != null && configured.isConfigurationSection(key))
            || (defaults != null && defaults.isConfigurationSection(key));
    }

    @NotNull
    Set<String> keys() {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        if (defaults != null) {
            keys.addAll(defaults.getKeys(false));
        }
        if (configured != null) {
            keys.addAll(configured.getKeys(false));
        }
        return Collections.unmodifiableSet(keys);
    }

    @NotNull
    String string(@NotNull String relativePath, @NotNull String explicitFallback) {
        Object configuredValue = configuredValue(relativePath);
        if (configuredValue != null) {
            if (configuredValue instanceof String configuredString) {
                return configuredString;
            }
            warnInvalidType(fullPath(relativePath), configuredValue, "string", explicitFallback);
            return stringDefault(relativePath, explicitFallback);
        }
        return stringDefault(relativePath, explicitFallback);
    }

    @NotNull
    String stringWithDefault(@NotNull String relativePath) {
        return string(relativePath, requireDefaultString(relativePath));
    }

    @Nullable
    String stringOrNull(@NotNull String relativePath) {
        Object configuredValue = configuredValue(relativePath);
        if (configuredValue != null) {
            if (configuredValue instanceof String configuredString) {
                return configuredString;
            }
            warnInvalidType(fullPath(relativePath), configuredValue, "string", null);
            return stringDefault(relativePath, null);
        }
        return stringDefault(relativePath, null);
    }

    boolean bool(@NotNull String relativePath, boolean explicitFallback) {
        Object configuredValue = configuredValue(relativePath);
        if (configuredValue != null) {
            if (configuredValue instanceof Boolean configuredBoolean) {
                return configuredBoolean;
            }
            if (configuredValue instanceof String configuredString) {
                if ("true".equalsIgnoreCase(configuredString)) {
                    return true;
                }
                if ("false".equalsIgnoreCase(configuredString)) {
                    return false;
                }
            }
            warnInvalidType(fullPath(relativePath), configuredValue, "boolean", boolDefault(relativePath, explicitFallback));
            return boolDefault(relativePath, explicitFallback);
        }
        return boolDefault(relativePath, explicitFallback);
    }

    boolean boolWithDefault(@NotNull String relativePath) {
        return bool(relativePath, requireDefaultBoolean(relativePath));
    }

    int integer(@NotNull String relativePath, int explicitFallback) {
        Object configuredValue = configuredValue(relativePath);
        if (configuredValue != null) {
            Integer parsed = parseInteger(configuredValue);
            if (parsed != null) {
                return parsed;
            }
            warnInvalidType(fullPath(relativePath), configuredValue, "integer", integerDefault(relativePath, explicitFallback));
            return integerDefault(relativePath, explicitFallback);
        }
        return integerDefault(relativePath, explicitFallback);
    }

    int integerWithDefault(@NotNull String relativePath) {
        return integer(relativePath, requireDefaultInteger(relativePath));
    }

    int integer(@NotNull String relativePath, int explicitFallback, int min) {
        int value = integer(relativePath, explicitFallback);
        int normalized = Math.max(min, value);
        if (value != normalized) {
            warnInvalidValue(fullPath(relativePath), value, normalized);
        }
        return normalized;
    }

    int integerWithDefault(@NotNull String relativePath, int min) {
        return integer(relativePath, requireDefaultInteger(relativePath), min);
    }

    int integer(@NotNull String relativePath, int explicitFallback, int min, int max) {
        int value = integer(relativePath, explicitFallback);
        int normalized = Math.max(min, Math.min(max, value));
        if (value != normalized) {
            warnInvalidValue(fullPath(relativePath), value, normalized);
        }
        return normalized;
    }

    int integerWithDefault(@NotNull String relativePath, int min, int max) {
        return integer(relativePath, requireDefaultInteger(relativePath), min, max);
    }

    long longValue(@NotNull String relativePath, long explicitFallback) {
        Object configuredValue = configuredValue(relativePath);
        if (configuredValue != null) {
            Long parsed = parseLong(configuredValue);
            if (parsed != null) {
                return parsed;
            }
            warnInvalidType(fullPath(relativePath), configuredValue, "long", longDefault(relativePath, explicitFallback));
            return longDefault(relativePath, explicitFallback);
        }
        return longDefault(relativePath, explicitFallback);
    }

    long longWithDefault(@NotNull String relativePath) {
        return longValue(relativePath, requireDefaultLong(relativePath));
    }

    double decimal(@NotNull String relativePath, double explicitFallback) {
        Object configuredValue = configuredValue(relativePath);
        if (configuredValue != null) {
            Double parsed = parseDouble(configuredValue);
            if (parsed != null) {
                return parsed;
            }
            warnInvalidType(fullPath(relativePath), configuredValue, "number", decimalDefault(relativePath, explicitFallback));
            return decimalDefault(relativePath, explicitFallback);
        }
        return decimalDefault(relativePath, explicitFallback);
    }

    double decimalWithDefault(@NotNull String relativePath) {
        return decimal(relativePath, requireDefaultDecimal(relativePath));
    }

    double decimal(@NotNull String relativePath, double explicitFallback, double min) {
        double value = decimal(relativePath, explicitFallback);
        double normalized = Math.max(min, value);
        if (value != normalized) {
            warnInvalidValue(fullPath(relativePath), value, normalized);
        }
        return normalized;
    }

    double decimalWithDefault(@NotNull String relativePath, double min) {
        return decimal(relativePath, requireDefaultDecimal(relativePath), min);
    }

    double decimal(@NotNull String relativePath, double explicitFallback, double min, double max) {
        double value = decimal(relativePath, explicitFallback);
        double normalized = Math.max(min, Math.min(max, value));
        if (value != normalized) {
            warnInvalidValue(fullPath(relativePath), value, normalized);
        }
        return normalized;
    }

    double decimalWithDefault(@NotNull String relativePath, double min, double max) {
        return decimal(relativePath, requireDefaultDecimal(relativePath), min, max);
    }

    @NotNull
    Duration duration(@NotNull String relativePath, @NotNull Duration explicitFallback) {
        Object configuredValue = configuredValue(relativePath);
        if (configuredValue != null) {
            if (configuredValue instanceof String configuredString) {
                Duration parsed = parseDurationValue(configuredString);
                if (parsed != null) {
                    if (parsed.isNegative()) {
                        warnInvalidValue(fullPath(relativePath), configuredValue, Duration.ZERO);
                        return Duration.ZERO;
                    }
                    return parsed;
                }
            } else {
                warnInvalidType(fullPath(relativePath), configuredValue, "duration string", durationDefault(relativePath, explicitFallback));
                return durationDefault(relativePath, explicitFallback);
            }
            warnInvalidValue(fullPath(relativePath), configuredValue, durationDefault(relativePath, explicitFallback));
            return durationDefault(relativePath, explicitFallback);
        }
        return durationDefault(relativePath, explicitFallback);
    }

    @NotNull
    Duration durationWithDefault(@NotNull String relativePath) {
        return duration(relativePath, requireDefaultDuration(relativePath));
    }

    @NotNull
    List<String> stringList(@NotNull String relativePath) {
        Object configuredValue = configuredValue(relativePath);
        if (configuredValue != null) {
            if (configuredValue instanceof List<?> configuredList) {
                return configuredList.stream().map(String::valueOf).toList();
            }
            warnInvalidType(fullPath(relativePath), configuredValue, "list", listDefault(relativePath));
            return listDefault(relativePath);
        }
        return listDefault(relativePath);
    }

    private Object configuredValue(String relativePath) {
        return configured != null && configured.contains(relativePath) ? configured.get(relativePath) : null;
    }

    private String fullPath(String relativePath) {
        return path.isEmpty() ? relativePath : path + "." + relativePath;
    }

    private String stringDefault(String relativePath, String explicitFallback) {
        Object defaultValue = defaults != null && defaults.contains(relativePath) ? defaults.get(relativePath) : null;
        return defaultValue instanceof String defaultString ? defaultString : explicitFallback;
    }

    @NotNull
    private String requireDefaultString(String relativePath) {
        Object defaultValue = requiredDefaultValue(relativePath);
        if (defaultValue instanceof String defaultString) {
            return defaultString;
        }
        throw new IllegalStateException("Bundled config default for '" + fullPath(relativePath) + "' is not a string.");
    }

    private boolean boolDefault(String relativePath, boolean explicitFallback) {
        Object defaultValue = defaults != null && defaults.contains(relativePath) ? defaults.get(relativePath) : null;
        if (defaultValue instanceof Boolean defaultBoolean) {
            return defaultBoolean;
        }
        if (defaultValue instanceof String defaultString) {
            if ("true".equalsIgnoreCase(defaultString)) {
                return true;
            }
            if ("false".equalsIgnoreCase(defaultString)) {
                return false;
            }
        }
        return explicitFallback;
    }

    private boolean requireDefaultBoolean(String relativePath) {
        Object defaultValue = requiredDefaultValue(relativePath);
        if (defaultValue instanceof Boolean defaultBoolean) {
            return defaultBoolean;
        }
        if (defaultValue instanceof String defaultString) {
            if ("true".equalsIgnoreCase(defaultString)) {
                return true;
            }
            if ("false".equalsIgnoreCase(defaultString)) {
                return false;
            }
        }
        throw new IllegalStateException("Bundled config default for '" + fullPath(relativePath) + "' is not a boolean.");
    }

    private int integerDefault(String relativePath, int explicitFallback) {
        Object defaultValue = defaults != null && defaults.contains(relativePath) ? defaults.get(relativePath) : null;
        Integer parsed = parseInteger(defaultValue);
        return parsed != null ? parsed : explicitFallback;
    }

    private int requireDefaultInteger(String relativePath) {
        Integer parsed = parseInteger(requiredDefaultValue(relativePath));
        if (parsed != null) {
            return parsed;
        }
        throw new IllegalStateException("Bundled config default for '" + fullPath(relativePath) + "' is not an integer.");
    }

    private long longDefault(String relativePath, long explicitFallback) {
        Object defaultValue = defaults != null && defaults.contains(relativePath) ? defaults.get(relativePath) : null;
        Long parsed = parseLong(defaultValue);
        return parsed != null ? parsed : explicitFallback;
    }

    private long requireDefaultLong(String relativePath) {
        Long parsed = parseLong(requiredDefaultValue(relativePath));
        if (parsed != null) {
            return parsed;
        }
        throw new IllegalStateException("Bundled config default for '" + fullPath(relativePath) + "' is not a long.");
    }

    private double decimalDefault(String relativePath, double explicitFallback) {
        Object defaultValue = defaults != null && defaults.contains(relativePath) ? defaults.get(relativePath) : null;
        Double parsed = parseDouble(defaultValue);
        return parsed != null ? parsed : explicitFallback;
    }

    private double requireDefaultDecimal(String relativePath) {
        Double parsed = parseDouble(requiredDefaultValue(relativePath));
        if (parsed != null) {
            return parsed;
        }
        throw new IllegalStateException("Bundled config default for '" + fullPath(relativePath) + "' is not a number.");
    }

    private Duration durationDefault(String relativePath, Duration explicitFallback) {
        Object defaultValue = defaults != null && defaults.contains(relativePath) ? defaults.get(relativePath) : null;
        if (defaultValue instanceof String defaultString) {
            Duration parsed = parseDurationValue(defaultString);
            if (parsed != null) {
                return parsed.isNegative() ? Duration.ZERO : parsed;
            }
        }
        return explicitFallback;
    }

    @NotNull
    private Duration requireDefaultDuration(String relativePath) {
        Object defaultValue = requiredDefaultValue(relativePath);
        if (defaultValue instanceof String defaultString) {
            Duration parsed = parseDurationValue(defaultString);
            if (parsed != null) {
                return parsed.isNegative() ? Duration.ZERO : parsed;
            }
        }
        throw new IllegalStateException("Bundled config default for '" + fullPath(relativePath) + "' is not a duration string.");
    }

    @NotNull
    private Object requiredDefaultValue(String relativePath) {
        if (defaults != null && defaults.contains(relativePath)) {
            return defaults.get(relativePath);
        }
        throw new IllegalStateException("Bundled config is missing default for '" + fullPath(relativePath) + "'.");
    }

    private List<String> listDefault(String relativePath) {
        Object defaultValue = defaults != null && defaults.contains(relativePath) ? defaults.get(relativePath) : null;
        if (defaultValue instanceof List<?> defaultList) {
            return defaultList.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private static Integer parseInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String string) {
            try {
                return Integer.parseInt(string.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Long parseLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String string) {
            try {
                return Long.parseLong(string.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Double parseDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String string) {
            try {
                return Double.parseDouble(string.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    @Nullable
    private static Duration parseDurationValue(String rawValue) {
        try {
            return ConfigDuration.parse(rawValue);
        } catch (Exception e) {
            return null;
        }
    }

    private void warnInvalidType(@NotNull String path, @NotNull Object configuredValue, @NotNull String expectedType, Object fallbackValue) {
        logger.warning("Config value '" + path + "' has invalid type " + typeOf(configuredValue)
            + "; expected " + expectedType + ". Using fallback " + fallbackDescription(fallbackValue) + ".");
    }

    private void warnInvalidValue(@NotNull String path, @NotNull Object configuredValue, Object fallbackValue) {
        logger.warning("Config value '" + path + "' has invalid value '" + configuredValue
            + "'. Using fallback " + fallbackDescription(fallbackValue) + ".");
    }

    @NotNull
    private static String typeOf(@NotNull Object configuredValue) {
        if (configuredValue instanceof MemorySection) {
            return "section";
        }
        if (configuredValue instanceof List<?>) {
            return "list";
        }
        return configuredValue.getClass().getSimpleName();
    }

    @NotNull
    private static String fallbackDescription(Object fallbackValue) {
        if (fallbackValue == null) {
            return "<none>";
        }
        if (fallbackValue instanceof String string) {
            return "'" + string + "'";
        }
        return String.valueOf(fallbackValue);
    }
}
