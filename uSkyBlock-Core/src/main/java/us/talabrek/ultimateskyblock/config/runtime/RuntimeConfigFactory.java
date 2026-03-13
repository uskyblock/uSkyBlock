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
        String configuredLanguage = config.getString("language", "en");
        int maxPartySize = Math.max(0, config.getInt("options.general.maxPartySize", 4));
        int distance = Math.max(50, config.getInt("options.island.distance", 110));
        int protectionRange = Math.min(distance, config.getInt("options.island.protectionRange", 128));
        int islandHeight = Math.max(20, config.getInt("options.island.height", 120));

        return new RuntimeConfig(
            configuredLanguage,
            loadLocale(configuredLanguage),
            new RuntimeConfig.General(
                maxPartySize,
                config.getString("options.general.worldName", "skyworld"),
                Math.max(0, config.getInt("options.general.cooldownInfo", 60)),
                parseDuration(config.getString("options.general.cooldownRestart"), Duration.ofHours(1), true),
                parseDuration(config.getString("options.general.biomeChange"), Duration.ofHours(1), true),
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
                loadStringLists(config.getConfigurationSection("options.island.extraPermissions")),
                config.getBoolean("options.island.allowIslandLock"),
                config.getBoolean("options.island.useIslandLevel"),
                config.getBoolean("options.island.useTopTen"),
                config.getString("options.island.schematicName", "default"),
                parseDuration(config.getString("options.island.topTenTimeout", "7m"), Duration.ofMinutes(7), false),
                parseDuration(config.getString("options.island.reservationTimeout"), Duration.ofMinutes(5), true),
                isAllowPvP(config),
                parseDuration(config.getString("options.island.islandTeleportDelay", "2s"), Duration.ofSeconds(2), false),
                config.getDouble("options.island.teleportCancelDistance", 0.2d),
                Math.max(0, config.getInt("options.island.autoRefreshScore", 0)),
                config.getBoolean("options.island.topTenShowMembers", true),
                Math.max(0, config.getInt("options.island.log-size", 10)),
                config.getBoolean("island-schemes-enabled", true),
                config.getString("options.island.chat-format", "&9SKY &r{DISPLAYNAME} &f>&d {MESSAGE}"),
                new RuntimeConfig.SpawnLimits(
                    config.getBoolean("options.island.spawn-limits.enabled", true),
                    config.getInt("options.island.spawn-limits.animals", 30),
                    config.getInt("options.island.spawn-limits.monsters", 50),
                    config.getInt("options.island.spawn-limits.villagers", 16),
                    config.getInt("options.island.spawn-limits.golems", 5),
                    config.getInt("options.island.spawn-limits.copper-golems", 5)
                ),
                loadIntMap(config.getConfigurationSection("options.island.block-limits"), "enabled")
            ),
            new RuntimeConfig.Extras(
                config.getBoolean("options.extras.sendToSpawn"),
                config.getBoolean("options.extras.respawnAtIsland"),
                config.getBoolean("options.extras.obsidianToLava", true)
            ),
            new RuntimeConfig.Protection(
                config.getBoolean("options.protection.enabled", true),
                config.getBoolean("options.protection.item-drops", true),
                config.getBoolean("options.protection.visitors.item-drops", true),
                config.getBoolean("options.protection.protect-lava", true),
                config.getBoolean("options.protection.visitors.fall", true),
                config.getBoolean("options.protection.visitors.fire-damage", true),
                config.getBoolean("options.protection.visitors.monster-damage", false),
                config.getBoolean("options.protection.visitors.warn-on-warp", true),
                config.getBoolean("options.protection.visitors.block-banned-entry", true)
            ),
            new RuntimeConfig.Nether(
                config.getBoolean("nether.enabled", false),
                config.getInt("nether.lava_level", config.getInt("nether.lava-level", 32)),
                config.getInt("nether.height", islandHeight / 2),
                config.getString("nether.chunk-generator", "us.talabrek.ultimateskyblock.world.SkyBlockNetherChunkGenerator")
            ),
            new RuntimeConfig.Restart(
                config.getBoolean("options.restart.clearInventory", true),
                config.getBoolean("options.restart.clearPerms", true),
                config.getBoolean("options.restart.clearArmor", true),
                config.getBoolean("options.restart.clearEnderChest", true),
                config.getBoolean("options.restart.clearCurrency", false),
                config.getBoolean("options.restart.teleportWhenReady", true),
                parseDuration(config.getString("options.restart.teleportDelay", "2000ms"), Duration.ofSeconds(2), false),
                List.copyOf(config.getStringList("options.restart.extra-commands"))
            ),
            new RuntimeConfig.Advanced(
                parseDuration(config.getString("options.advanced.confirmTimeout", "10s"), Duration.ofSeconds(10), false),
                config.getBoolean("options.advanced.useDisplayNames", false),
                config.getDouble("options.advanced.topTenCutoff", config.getDouble("options.advanced.purgeLevel", 10)),
                config.getBoolean("options.advanced.manageSpawn", true),
                config.getString("options.advanced.playerCache", "maximumSize=200,expireAfterWrite=15m,expireAfterAccess=10m"),
                config.getString("options.advanced.islandCache", "maximumSize=200,expireAfterWrite=15m,expireAfterAccess=10m"),
                config.getString("options.advanced.placeholderCache", "maximumSize=200,expireAfterWrite=20s"),
                config.getString("options.advanced.completionCache", "maximumSize=200,expireAfterWrite=15m,expireAfterAccess=10m"),
                Duration.ofSeconds(config.getInt("options.advanced.island.saveEvery", 30)),
                Duration.ofSeconds(config.getInt("options.advanced.player.saveEvery", 2 * 60)),
                config.getString("options.advanced.chunk-generator", "us.talabrek.ultimateskyblock.world.SkyBlockChunkGenerator"),
                Math.max(0, config.getInt("options.advanced.chunkRegenSpeed", 4)),
                Duration.ofMillis(config.getLong("async.long.feedbackEvery", 30000)),
                config.getDouble("options.advanced.purgeLevel", 10),
                Duration.ofMillis(config.getLong("options.advanced.purgeTimeout", 600000)),
                normalizeBlank(config.getString("options.advanced.debugLevel")),
                new RuntimeConfig.PlayerDb(
                    config.getString("options.advanced.playerdb.storage", "yml"),
                    config.getString("options.advanced.playerdb.nameCache", "maximumSize=1500,expireAfterWrite=30m,expireAfterAccess=15m"),
                    config.getString("options.advanced.playerdb.uuidCache", "maximumSize=1500,expireAfterWrite=30m,expireAfterAccess=15m"),
                    Duration.ofMillis(config.getInt("playerdb.saveDelay", 10000))
                )
            ),
            new RuntimeConfig.Async(
                Duration.ofMillis(config.getInt("async.maxMs", 15)),
                config.getLong("async.maxConsecutiveTicks", 20),
                TimeUtil.ticksAsDuration(config.getLong("async.yieldDelay", 2))
            ),
            new RuntimeConfig.AsyncWorldEdit(
                config.getBoolean("asyncworldedit.enabled", true),
                Duration.ofMillis(config.getInt("asyncworldedit.watchDog.heartBeatMs", 2000)),
                parseDuration(config.getString("asyncworldedit.watchDog.timeout", "5m"), Duration.ofMinutes(5), false)
            ),
            new RuntimeConfig.Party(
                parseDuration(config.getString("options.party.invite-timeout", "2m"), Duration.ofMinutes(2), false),
                config.getString("options.party.chat-format", "&9PARTY &r{DISPLAYNAME} &f>&b {MESSAGE}"),
                List.copyOf(config.getStringList("options.party.join-commands")),
                List.copyOf(config.getStringList("options.party.leave-commands")),
                loadPartyPermissionOverrides(config.getConfigurationSection("options.party.maxPartyPermissions"))
            ),
            new RuntimeConfig.PluginUpdates(
                config.getBoolean("plugin-updates.check", true),
                config.getString("plugin-updates.branch", "RELEASE")
            ),
            new RuntimeConfig.Spawning(
                new RuntimeConfig.Guardians(
                    config.getBoolean("options.spawning.guardians.enabled", true),
                    Math.max(0, config.getInt("options.spawning.guardians.max-per-island", 10)),
                    config.getDouble("options.spawning.guardians.spawn-chance", 0.10d)
                ),
                new RuntimeConfig.Phantoms(
                    config.getBoolean("options.spawning.phantoms.overworld", true),
                    config.getBoolean("options.spawning.phantoms.nether", false)
                )
            ),
            new RuntimeConfig.Placeholder(
                config.getBoolean("placeholder.chatplaceholder", false),
                config.getBoolean("placeholder.servercommandplaceholder", false),
                config.getBoolean("placeholder.mvdwplaceholderapi", false)
            ),
            new RuntimeConfig.ToolMenu(
                config.getBoolean("tool-menu.enabled", true),
                config.getString("tool-menu.tool", "OAK_SAPLING"),
                loadStringMap(config.getConfigurationSection("tool-menu.commands"))
            ),
            new RuntimeConfig.Signs(config.getBoolean("signs.enabled", true)),
            new RuntimeConfig.WorldGuard(
                config.getBoolean("worldguard.entry-message", true),
                config.getBoolean("worldguard.exit-message", true)
            ),
            new RuntimeConfig.Importer(
                config.getDouble("importer.progressEveryPct", 10),
                Duration.ofMillis(config.getLong("importer.progressEveryMs", 10000))
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
