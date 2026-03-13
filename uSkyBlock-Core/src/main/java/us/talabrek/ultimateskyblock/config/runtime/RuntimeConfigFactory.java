package us.talabrek.ultimateskyblock.config.runtime;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.config.ConfigDuration;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class RuntimeConfigFactory {
    private RuntimeConfigFactory() {
    }

    @NotNull
    public static RuntimeConfig load(@NotNull FileConfiguration config) {
        int maxPartySize = Math.max(0, config.getInt("options.general.maxPartySize", 4));
        int distance = Math.max(50, config.getInt("options.island.distance", 110));
        int protectionRange = Math.min(distance, config.getInt("options.island.protectionRange", 128));
        int cooldownInfo = Math.max(0, config.getInt("options.general.cooldownInfo", 60));
        Duration biomeChange = parseDuration(config.getString("options.general.biomeChange"), Duration.ofHours(1), true);
        Duration cooldownRestart = parseDuration(config.getString("options.general.cooldownRestart"), Duration.ofHours(1), true);
        int islandHeight = Math.max(20, config.getInt("options.island.height", 120));
        Duration topTenTimeout = parseDuration(config.getString("options.island.topTenTimeout", "7m"), Duration.ofMinutes(7), false);
        Duration teleportDelay = parseDuration(config.getString("options.island.islandTeleportDelay", "2s"), Duration.ofSeconds(2), false);
        Duration confirmTimeout = parseDuration(config.getString("options.advanced.confirmTimeout", "10s"), Duration.ofSeconds(10), false);
        Duration inviteTimeout = parseDuration(config.getString("options.party.invite-timeout", "2m"), Duration.ofMinutes(2), false);

        return new RuntimeConfig(
            loadLocale(config),
            new RuntimeConfig.General(
                maxPartySize,
                config.getString("options.general.worldName", "skyworld"),
                cooldownInfo,
                cooldownRestart,
                biomeChange,
                config.getString("options.general.defaultBiome", "ocean"),
                config.getString("options.general.defaultNetherBiome", "nether_wastes"),
                config.getInt("options.general.spawnSize", 50),
                config.getInt("general.maxSpam", 3000)
            ),
            new RuntimeConfig.Island(
                distance,
                islandHeight,
                config.getBoolean("options.island.removeCreaturesByTeleport"),
                protectionRange,
                protectionRange / 2,
                List.copyOf(config.getStringList("options.island.chestItems")),
                config.getBoolean("options.island.addExtraItems"),
                loadExtraPermissions(config),
                config.getBoolean("options.island.allowIslandLock"),
                config.getBoolean("options.island.useIslandLevel"),
                config.getBoolean("options.island.useTopTen"),
                config.getString("options.island.schematicName", "default"),
                topTenTimeout,
                isAllowPvP(config),
                teleportDelay,
                config.getDouble("options.island.teleportCancelDistance", 0.2d),
                config.getBoolean("options.island.spawn-limits.enabled", true)
            ),
            new RuntimeConfig.Extras(
                config.getBoolean("options.extras.sendToSpawn"),
                config.getBoolean("options.extras.respawnAtIsland"),
                config.getBoolean("options.extras.obsidianToLava", true)
            ),
            new RuntimeConfig.Nether(
                config.getBoolean("nether.enabled", false),
                config.getInt("nether.lava_level", config.getInt("nether.lava-level", 32)),
                config.getInt("nether.height", islandHeight / 2)
            ),
            new RuntimeConfig.Advanced(confirmTimeout),
            new RuntimeConfig.Party(inviteTimeout),
            new RuntimeConfig.PluginUpdates(config.getString("plugin-updates.branch", "RELEASE")),
            new RuntimeConfig.Spawning(new RuntimeConfig.Guardians(
                config.getBoolean("options.spawning.guardians.enabled", true),
                Math.max(0, config.getInt("options.spawning.guardians.max-per-island", 10)),
                config.getDouble("options.spawning.guardians.spawn-chance", 0.10d)
            )),
            loadIslandSchemes(config),
            loadConfirmations(config)
        );
    }

    @NotNull
    private static Locale loadLocale(@NotNull FileConfiguration config) {
        Locale configured = I18nUtil.getLocale(config.getString("language", null));
        return configured != null ? configured : Locale.getDefault();
    }

    @NotNull
    private static Duration parseDuration(String rawValue, @NotNull Duration defaultValue, boolean clampNegative) {
        try {
            Duration parsed = ConfigDuration.parse(rawValue);
            return clampNegative && parsed.isNegative() ? Duration.ZERO : parsed;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @NotNull
    private static Set<String> loadExtraPermissions(@NotNull FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("options.island.extraPermissions");
        if (section == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(section.getKeys(false)));
    }

    private static boolean isAllowPvP(@NotNull FileConfiguration config) {
        String value = config.getString("options.island.allowPvP", "deny");
        return "allow".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);
    }

    @NotNull
    private static Map<String, RuntimeConfig.IslandScheme> loadIslandSchemes(@NotNull FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("island-schemes");
        if (section == null) {
            return Collections.emptyMap();
        }

        Map<String, RuntimeConfig.IslandScheme> schemes = new LinkedHashMap<>();
        for (String schemeName : section.getKeys(false)) {
            ConfigurationSection schemeSection = section.getConfigurationSection(schemeName);
            if (schemeSection == null) {
                continue;
            }
            schemes.put(schemeName, new RuntimeConfig.IslandScheme(
                schemeSection.getBoolean("enabled", true),
                normalizeBlank(schemeSection.getString("schematic")),
                normalizeBlank(schemeSection.getString("nether-schematic"))
            ));
        }
        return Collections.unmodifiableMap(schemes);
    }

    @NotNull
    private static Map<String, Boolean> loadConfirmations(@NotNull FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("confirmation");
        if (section == null) {
            return Collections.emptyMap();
        }

        Map<String, Boolean> confirmations = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            confirmations.put(key, section.getBoolean(key, true));
        }
        return Collections.unmodifiableMap(confirmations);
    }

    private static String normalizeBlank(String value) {
        return value != null && !value.trim().isEmpty() ? value : null;
    }
}
