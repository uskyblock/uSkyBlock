package us.talabrek.ultimateskyblock.config.runtime;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import dk.lockfuglsang.minecraft.util.TimeUtil;
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

public final class RuntimeConfigFactory {
    private RuntimeConfigFactory() {
    }

    @NotNull
    public static RuntimeConfig load(@NotNull FileConfiguration config) {
        // Nominal defaults come from the bundled config.yml attached by PluginConfigLoader.
        // Keep inline fallbacks here only for malformed values or legacy compatibility paths,
        // not as a second source of truth for ordinary defaults.
        String configuredLanguage = normalizeConfiguredLanguage(config.getString("language"));
        int maxPartySize = Math.max(0, config.getInt("options.general.maxPartySize"));
        int distance = Math.max(50, config.getInt("options.island.distance"));
        int protectionRange = Math.min(distance, config.getInt("options.island.protectionRange"));
        int islandHeight = Math.max(20, config.getInt("options.island.height"));

        return new RuntimeConfig(
            configuredLanguage,
            loadLocale(configuredLanguage),
            new RuntimeConfig.Init(TimeUtil.ticksAsDuration(config.getLong("init.initDelay"))),
            new RuntimeConfig.General(
                maxPartySize,
                config.getString("options.general.worldName"),
                Math.max(0, config.getInt("options.general.cooldownInfo")),
                parseDuration(config, "options.general.cooldownRestart", Duration.ofHours(1), true),
                parseDuration(config, "options.general.biomeChange", Duration.ofHours(1), true),
                config.getString("options.general.defaultBiome"),
                config.getString("options.general.defaultNetherBiome"),
                config.getInt("options.general.spawnSize"),
                config.getInt("options.general.maxSpam")
            ),
            new RuntimeConfig.Island(
                distance,
                islandHeight,
                config.getBoolean("options.island.removeCreaturesByTeleport"),
                protectionRange,
                protectionRange / 2,
                List.copyOf(config.getStringList("options.island.chestItems")),
                config.getBoolean("options.island.addExtraItems"),
                loadStringLists(config.getConfigurationSection("options.island.extraPermissions")),
                config.getBoolean("options.island.allowIslandLock"),
                config.getBoolean("options.island.useIslandLevel"),
                config.getBoolean("options.island.useTopTen"),
                config.getString("options.island.schematicName"),
                parseDuration(config, "options.island.topTenTimeout", Duration.ofMinutes(7), false),
                parseDuration(config, "options.island.reservationTimeout", Duration.ofMinutes(5), true),
                isAllowPvP(config),
                parseDuration(config, "options.island.islandTeleportDelay", Duration.ofSeconds(2), false),
                config.getDouble("options.island.teleportCancelDistance"),
                Math.max(0, config.getInt("options.island.autoRefreshScore")),
                config.getBoolean("options.island.topTenShowMembers"),
                Math.max(0, config.getInt("options.island.log-size")),
                config.getBoolean("island-schemes-enabled"),
                config.getString("options.island.chat-format"),
                new RuntimeConfig.SpawnLimits(
                    config.getBoolean("options.island.spawn-limits.enabled"),
                    config.getInt("options.island.spawn-limits.animals"),
                    config.getInt("options.island.spawn-limits.monsters"),
                    config.getInt("options.island.spawn-limits.villagers"),
                    config.getInt("options.island.spawn-limits.golems"),
                    config.getInt("options.island.spawn-limits.copper-golems")
                ),
                loadIntMap(config.getConfigurationSection("options.island.block-limits"), "enabled")
            ),
            new RuntimeConfig.Extras(
                config.getBoolean("options.extras.sendToSpawn"),
                config.getBoolean("options.extras.respawnAtIsland"),
                config.getBoolean("options.extras.obsidianToLava")
            ),
            new RuntimeConfig.Protection(
                config.getBoolean("options.protection.enabled"),
                config.getBoolean("options.protection.item-drops"),
                config.getBoolean("options.protection.visitors.item-drops"),
                config.getBoolean("options.protection.creepers"),
                config.getBoolean("options.protection.withers"),
                config.getBoolean("options.protection.protect-lava"),
                config.getBoolean("options.protection.visitors.trampling"),
                config.getBoolean("options.protection.visitors.kill-animals"),
                config.getBoolean("options.protection.visitors.kill-monsters"),
                config.getBoolean("options.protection.visitors.shearing"),
                config.getBoolean("options.protection.visitors.hatching"),
                config.getBoolean("options.protection.visitors.fall"),
                config.getBoolean("options.protection.visitors.fire-damage"),
                config.getBoolean("options.protection.visitors.monster-damage"),
                config.getBoolean("options.protection.visitors.warn-on-warp"),
                config.getBoolean("options.protection.visitors.villager-trading"),
                config.getBoolean("options.protection.villager-trading-enabled"),
                config.getBoolean("options.protection.visitors.use-portals"),
                config.getBoolean("options.protection.visitors.vehicle-enter"),
                config.getBoolean("options.protection.visitors.vehicle-damage"),
                config.getBoolean("options.protection.visitors.block-banned-entry")
            ),
            new RuntimeConfig.Nether(
                config.getBoolean("nether.enabled", false),
                config.getInt("nether.lava_level", config.getInt("nether.lava-level", 32)),
                config.getInt("nether.height", islandHeight / 2),
                config.getString("nether.chunk-generator"),
                new RuntimeConfig.Terraform(
                    config.getBoolean("nether.terraform-enabled"),
                    config.getDouble("nether.terraform-min-pitch"),
                    config.getDouble("nether.terraform-max-pitch"),
                    Math.max(0, config.getInt("nether.terraform-distance")),
                    loadStringLists(config.getConfigurationSection("nether.terraform")),
                    loadDoubleMap(config.getConfigurationSection("nether.terraform-weight"), 1d)
                ),
                new RuntimeConfig.SpawnChances(
                    config.getBoolean("nether.spawn-chances.enabled"),
                    config.getDouble("nether.spawn-chances.blaze"),
                    config.getDouble("nether.spawn-chances.wither"),
                    config.getDouble("nether.spawn-chances.skeleton")
                )
            ),
            new RuntimeConfig.Restart(
                config.getBoolean("options.restart.clearInventory"),
                config.getBoolean("options.restart.clearPerms"),
                config.getBoolean("options.restart.clearArmor"),
                config.getBoolean("options.restart.clearEnderChest"),
                config.getBoolean("options.restart.clearCurrency"),
                config.getBoolean("options.restart.teleportWhenReady"),
                parseDuration(config, "options.restart.teleportDelay", Duration.ofSeconds(2), false),
                List.copyOf(config.getStringList("options.restart.extra-commands"))
            ),
            new RuntimeConfig.Advanced(
                parseDuration(config, "options.advanced.confirmTimeout", Duration.ofSeconds(10), false),
                config.getBoolean("options.advanced.useDisplayNames"),
                config.getDouble("options.advanced.topTenCutoff", config.getDouble("options.advanced.purgeLevel")),
                config.getBoolean("options.advanced.manageSpawn"),
                config.getString("options.advanced.playerCache"),
                config.getString("options.advanced.islandCache"),
                config.getString("options.advanced.placeholderCache"),
                config.getString("options.advanced.completionCache"),
                Duration.ofSeconds(config.getInt("options.advanced.island.saveEvery")),
                Duration.ofSeconds(config.getInt("options.advanced.player.saveEvery")),
                config.getString("options.advanced.chunk-generator"),
                Math.max(0, config.getInt("options.advanced.chunkRegenSpeed")),
                Duration.ofMillis(config.getLong("async.long.feedbackEvery")),
                config.getDouble("options.advanced.purgeLevel"),
                Duration.ofMillis(config.getLong("options.advanced.purgeTimeout")),
                normalizeBlank(config.getString("options.advanced.debugLevel")),
                new RuntimeConfig.PlayerDb(
                    config.getString("options.advanced.playerdb.storage"),
                    config.getString("options.advanced.playerdb.nameCache"),
                    config.getString("options.advanced.playerdb.uuidCache"),
                    Duration.ofMillis(config.getInt("playerdb.saveDelay"))
                )
            ),
            new RuntimeConfig.Async(
                Duration.ofMillis(config.getInt("async.maxMs")),
                config.getLong("async.maxConsecutiveTicks"),
                TimeUtil.ticksAsDuration(config.getLong("async.yieldDelay"))
            ),
            new RuntimeConfig.AsyncWorldEdit(
                config.getBoolean("asyncworldedit.enabled"),
                Duration.ofMillis(config.getInt("asyncworldedit.watchDog.heartBeatMs")),
                parseDuration(config, "asyncworldedit.watchDog.timeout", Duration.ofMinutes(5), false)
            ),
            new RuntimeConfig.Party(
                parseDuration(config, "options.party.invite-timeout", Duration.ofMinutes(2), false),
                config.getString("options.party.chat-format"),
                List.copyOf(config.getStringList("options.party.join-commands")),
                List.copyOf(config.getStringList("options.party.leave-commands")),
                loadPartyPermissionOverrides(config.getConfigurationSection("options.party.maxPartyPermissions"))
            ),
            new RuntimeConfig.PluginUpdates(
                config.getBoolean("plugin-updates.check"),
                config.getString("plugin-updates.branch")
            ),
            new RuntimeConfig.Spawning(
                new RuntimeConfig.Guardians(
                    config.getBoolean("options.spawning.guardians.enabled"),
                    Math.max(0, config.getInt("options.spawning.guardians.max-per-island")),
                    config.getDouble("options.spawning.guardians.spawn-chance")
                ),
                new RuntimeConfig.Phantoms(
                    config.getBoolean("options.spawning.phantoms.overworld"),
                    config.getBoolean("options.spawning.phantoms.nether")
                )
            ),
            new RuntimeConfig.Placeholder(
                config.getBoolean("placeholder.chatplaceholder"),
                config.getBoolean("placeholder.servercommandplaceholder"),
                config.getBoolean("placeholder.mvdwplaceholderapi")
            ),
            new RuntimeConfig.ToolMenu(
                config.getBoolean("tool-menu.enabled"),
                config.getString("tool-menu.tool"),
                loadStringMap(config.getConfigurationSection("tool-menu.commands"))
            ),
            new RuntimeConfig.Signs(config.getBoolean("signs.enabled")),
            new RuntimeConfig.WorldGuard(
                config.getBoolean("worldguard.entry-message"),
                config.getBoolean("worldguard.exit-message")
            ),
            new RuntimeConfig.Importer(
                config.getDouble("importer.progressEveryPct"),
                Duration.ofMillis(config.getLong("importer.progressEveryMs"))
            ),
            loadIslandSchemes(config),
            loadExtraMenus(config.getConfigurationSection("options.extra-menus")),
            loadDonorPerks(config.getConfigurationSection("donor-perks")),
            loadConfirmations(config)
        );
    }

    @NotNull
    private static Locale loadLocale(@NotNull String configuredLanguage) {
        Locale configured = I18nUtil.getLocale(configuredLanguage);
        return configured != null ? configured : Locale.ENGLISH;
    }

    @NotNull
    private static String normalizeConfiguredLanguage(String configuredLanguage) {
        if (configuredLanguage == null || configuredLanguage.isBlank()) {
            return "en";
        }
        return I18nUtil.findSupportedLocaleKey(configuredLanguage).orElse("en");
    }

    @NotNull
    private static Duration parseDuration(@NotNull FileConfiguration config, @NotNull String path, @NotNull Duration fallbackValue, boolean clampNegative) {
        String defaultRawValue = config.getDefaults() != null ? config.getDefaults().getString(path) : null;
        Duration defaultValue = parseDurationValue(defaultRawValue, fallbackValue, clampNegative);
        return parseDurationValue(config.getString(path), defaultValue, clampNegative);
    }

    @NotNull
    private static Duration parseDurationValue(String rawValue, @NotNull Duration defaultValue, boolean clampNegative) {
        try {
            Duration parsed = ConfigDuration.parse(rawValue);
            return clampNegative && parsed.isNegative() ? Duration.ZERO : parsed;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @NotNull
    private static Map<String, List<String>> loadStringLists(ConfigurationSection section) {
        if (section == null) {
            return Collections.emptyMap();
        }

        Map<String, List<String>> values = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            values.put(key, List.copyOf(section.getStringList(key)));
        }
        return Collections.unmodifiableMap(values);
    }

    @NotNull
    private static Map<String, Integer> loadIntMap(ConfigurationSection section, String... ignoredKeys) {
        if (section == null) {
            return Collections.emptyMap();
        }

        LinkedHashSet<String> ignored = new LinkedHashSet<>(List.of(ignoredKeys));
        Map<String, Integer> values = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            if (ignored.contains(key) || !section.isInt(key)) {
                continue;
            }
            values.put(key, section.getInt(key));
        }
        return Collections.unmodifiableMap(values);
    }

    @NotNull
    private static Map<String, String> loadStringMap(ConfigurationSection section) {
        if (section == null) {
            return Collections.emptyMap();
        }

        Map<String, String> values = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            if (section.isString(key)) {
                values.put(key, section.getString(key));
            }
        }
        return Collections.unmodifiableMap(values);
    }

    @NotNull
    private static Map<String, Double> loadDoubleMap(ConfigurationSection section, double defaultValue) {
        if (section == null) {
            return Collections.emptyMap();
        }

        Map<String, Double> values = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            values.put(key, section.getDouble(key, defaultValue));
        }
        return Collections.unmodifiableMap(values);
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
                normalizeBlank(schemeSection.getString("nether-schematic")),
                schemeSection.getString("permission", "usb.schematic." + schemeName),
                normalizeBlank(schemeSection.getString("description")),
                schemeSection.getString("displayItem", "OAK_SAPLING"),
                Math.max(1, schemeSection.getInt("index", 1)),
                List.copyOf(schemeSection.getStringList("extraItems")),
                schemeSection.getInt("maxPartySize", 0),
                schemeSection.getInt("animals", 0),
                schemeSection.getInt("monsters", 0),
                schemeSection.getInt("villagers", 0),
                schemeSection.getInt("golems", 0),
                schemeSection.getInt("copper-golems", 0),
                schemeSection.getDouble("scoreMultiply", 1d),
                schemeSection.getDouble("scoreOffset", 0d)
            ));
        }
        return Collections.unmodifiableMap(schemes);
    }

    @NotNull
    private static Map<Integer, RuntimeConfig.ExtraMenu> loadExtraMenus(ConfigurationSection section) {
        if (section == null) {
            return Collections.emptyMap();
        }

        Map<Integer, RuntimeConfig.ExtraMenu> menus = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection menuSection = section.getConfigurationSection(key);
            if (menuSection == null) {
                continue;
            }
            try {
                int index = Integer.parseInt(key, 10);
                menus.put(index, new RuntimeConfig.ExtraMenu(
                    menuSection.getString("title", "\u00a9Unknown"),
                    menuSection.getString("displayItem", "CHEST"),
                    List.copyOf(menuSection.getStringList("lore")),
                    List.copyOf(menuSection.getStringList("commands"))
                ));
            } catch (NumberFormatException ignored) {
            }
        }
        return Collections.unmodifiableMap(menus);
    }

    @NotNull
    private static Map<String, RuntimeConfig.PerkSpec> loadDonorPerks(ConfigurationSection section) {
        if (section == null) {
            return Collections.emptyMap();
        }

        Map<String, RuntimeConfig.PerkSpec> perks = new LinkedHashMap<>();
        loadDonorPerks(section, null, perks);
        return Collections.unmodifiableMap(perks);
    }

    private static void loadDonorPerks(ConfigurationSection section, String permission, Map<String, RuntimeConfig.PerkSpec> perks) {
        if (isLeafSection(section)) {
            perks.put(permission, new RuntimeConfig.PerkSpec(
                List.copyOf(section.getStringList("extraItems")),
                section.getInt("maxPartySize", 0),
                section.getInt("animals", 0),
                section.getInt("monsters", 0),
                section.getInt("villagers", 0),
                section.getInt("golems", 0),
                section.getInt("copper-golems", 0),
                section.getDouble("rewardBonus", 0d),
                section.getDouble("hungerReduction", 0d),
                List.copyOf(section.getStringList("schematics"))
            ));
            return;
        }

        for (String key : section.getKeys(false)) {
            if (!section.isConfigurationSection(key)) {
                continue;
            }
            ConfigurationSection child = section.getConfigurationSection(key);
            loadDonorPerks(child, permission != null ? permission + "." + key : key, perks);
        }
    }

    private static boolean isLeafSection(ConfigurationSection section) {
        for (String key : section.getKeys(false)) {
            if (!section.isConfigurationSection(key)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    private static Map<String, Integer> loadPartyPermissionOverrides(ConfigurationSection section) {
        Map<String, Integer> values = new LinkedHashMap<>();
        if (section != null) {
            loadPartyPermissionOverrides(section, null, values);
        }
        return Collections.unmodifiableMap(values);
    }

    private static void loadPartyPermissionOverrides(ConfigurationSection section, String permission, Map<String, Integer> values) {
        for (String key : section.getKeys(false)) {
            if (section.isConfigurationSection(key)) {
                loadPartyPermissionOverrides(section.getConfigurationSection(key), permission != null ? permission + "." + key : key, values);
            } else if (section.isInt(key) && permission != null) {
                values.put(permission, section.getInt(key, 0));
            }
        }
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
