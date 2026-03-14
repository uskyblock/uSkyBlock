package us.talabrek.ultimateskyblock.config.runtime;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.po.I18nUtil;
import org.bukkit.Registry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.gameobject.ItemStackAmountSpec;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

@Singleton
public final class RuntimeConfigFactory {
    private static final Duration DEFAULT_INIT_DELAY = Duration.ofMillis(2500);
    private static final String DEFAULT_PLAYER_CACHE_SPEC = "maximumSize=200,expireAfterWrite=15m,expireAfterAccess=10m";
    private static final String DEFAULT_ISLAND_CACHE_SPEC = "maximumSize=200,expireAfterWrite=15m,expireAfterAccess=10m";
    private static final String DEFAULT_PLACEHOLDER_CACHE_SPEC = "maximumSize=200,expireAfterWrite=20s";
    private static final String DEFAULT_COMPLETION_CACHE_SPEC = "maximumSize=200,expireAfterWrite=15m,expireAfterAccess=10m";
    private static final Duration DEFAULT_ISLAND_SAVE_EVERY = Duration.ofSeconds(30);
    private static final Duration DEFAULT_PLAYER_SAVE_EVERY = Duration.ofSeconds(120);
    private static final String DEFAULT_OVERWORLD_CHUNK_GENERATOR = "us.talabrek.ultimateskyblock.world.SkyBlockChunkGenerator";
    private static final String DEFAULT_NETHER_CHUNK_GENERATOR = "us.talabrek.ultimateskyblock.world.SkyBlockNetherChunkGenerator";
    private static final Duration DEFAULT_LONG_FEEDBACK_EVERY = Duration.ofSeconds(30);
    private static final Duration DEFAULT_PURGE_TIMEOUT = Duration.ofMinutes(10);
    private static final String DEFAULT_PLAYERDB_NAME_CACHE_SPEC = "maximumSize=1500,expireAfterWrite=30m,expireAfterAccess=15m";
    private static final String DEFAULT_PLAYERDB_UUID_CACHE_SPEC = "maximumSize=1500,expireAfterWrite=30m,expireAfterAccess=15m";
    private static final Duration DEFAULT_PLAYERDB_SAVE_DELAY = Duration.ofSeconds(10);
    private static final Duration DEFAULT_ASYNC_MAX_ITERATION_TIME = Duration.ofMillis(15);
    private static final long DEFAULT_ASYNC_MAX_CONSECUTIVE_TICKS = 20L;
    private static final Duration DEFAULT_ASYNC_YIELD_DELAY = Duration.ofMillis(100);
    private static final double DEFAULT_IMPORTER_PROGRESS_EVERY_FRACTION = 0.10d;
    private static final Duration DEFAULT_IMPORTER_PROGRESS_EVERY = Duration.ofSeconds(10);
    private static final Set<String> VALID_UPDATE_BRANCHES = Set.of("RELEASE", "STAGING");

    private final GameObjectFactory gameObjects;
    private final Logger logger;
    private final YamlConfiguration bundledDefaults;

    @Inject
    public RuntimeConfigFactory(@NotNull GameObjectFactory gameObjects, @NotNull Logger logger) {
        this.gameObjects = gameObjects;
        this.logger = logger;
        this.bundledDefaults = PluginConfigLoader.loadBundledConfig();
    }

    @NotNull
    public RuntimeConfig load(@NotNull FileConfiguration config) {
        // Custom config loading bypassing Bukkit's default convenience methods which silently swallow misconfigurations.
        // The strategy is: Precisely get the configured value. If it is invalid, log a warning so admins are aware.
        // Then try to find a sane fallback explicitly; either the default config value or more complex fallback if required.
        // Only if the misconfiguration is unrecoverable, fail startup. This should be rare to avoid frustration.
        // TL;DR: never silently swallow misconfiguration, try hard to recover if possible, fail explicitly if unrecoverable.
        RuntimeConfigNode root = RuntimeConfigNode.root(config, bundledDefaults, logger);

        String configuredLanguage = resolveConfiguredLanguage(root.stringOrNull("language"));
        int maxPartySize = root.integerWithDefault("options.general.maxPartySize", 0);
        int distance = root.integerWithDefault("options.island.distance", 50);
        int configuredProtectionRange = root.integerWithDefault("options.island.protectionRange", 0);
        int protectionRange = Math.min(distance, configuredProtectionRange);
        warnIfNormalized("options.island.protectionRange", configuredProtectionRange, protectionRange);
        int islandHeight = root.integerWithDefault("options.island.height", 20);
        String defaultBiomeKey = root.stringWithDefault("options.general.defaultBiome");
        warnIfInvalidBiomeKey("options.general.defaultBiome", defaultBiomeKey);
        String defaultNetherBiomeKey = root.stringWithDefault("options.general.defaultNetherBiome");
        warnIfInvalidBiomeKey("options.general.defaultNetherBiome", defaultNetherBiomeKey);
        String pluginUpdateBranch = normalizeUpdateBranch(root.stringWithDefault("plugin-updates.branch"));
        double guardianSpawnChance = root.decimalWithDefault("options.spawning.guardians.spawn-chance", 0d, 1d);

        return new RuntimeConfig(
                configuredLanguage,
                loadLocale(configuredLanguage),
                new RuntimeConfig.Init(root.duration("init.initDelay", DEFAULT_INIT_DELAY)),
                new RuntimeConfig.General(
                    maxPartySize,
                    root.stringWithDefault("options.general.worldName"),
                    root.durationWithDefault("options.general.cooldownInfo"),
                    root.durationWithDefault("options.general.cooldownRestart"),
                    root.durationWithDefault("options.general.biomeChange"),
                    defaultBiomeKey,
                    defaultNetherBiomeKey,
                    root.integerWithDefault("options.general.spawnSize", 0),
                    root.durationWithDefault("options.general.maxSpam")
                ),
                new RuntimeConfig.Island(
                    distance,
                    islandHeight,
                    root.boolWithDefault("options.island.removeCreaturesByTeleport"),
                    protectionRange,
                    protectionRange / 2,
                    gameObjects.itemStackAmounts(root.stringList("options.island.chestItems")),
                    root.boolWithDefault("options.island.addExtraItems"),
                    loadItemStackAmountLists(root.child("options.island.extraPermissions")),
                    root.boolWithDefault("options.island.allowIslandLock"),
                    root.boolWithDefault("options.island.useIslandLevel"),
                    root.boolWithDefault("options.island.useTopTen"),
                    root.stringWithDefault("options.island.default-scheme"),
                    root.durationWithDefault("options.island.topTenTimeout"),
                    root.duration("options.island.reservationTimeout", Duration.ofMinutes(5)),
                    isAllowPvP(root),
                    root.durationWithDefault("options.island.islandTeleportDelay"),
                    root.decimalWithDefault("options.island.teleportCancelDistance", 0d),
                    root.durationWithDefault("options.island.autoRefreshScore"),
                    root.boolWithDefault("options.island.topTenShowMembers"),
                    root.integerWithDefault("options.island.log-size", 0),
                    root.boolWithDefault("island-schemes-enabled"),
                    root.stringWithDefault("options.island.chat-format"),
                    new RuntimeConfig.SpawnLimits(
                        root.boolWithDefault("options.island.spawn-limits.enabled"),
                        root.integerWithDefault("options.island.spawn-limits.animals", 0),
                        root.integerWithDefault("options.island.spawn-limits.monsters", 0),
                        root.integerWithDefault("options.island.spawn-limits.villagers", 0),
                        root.integerWithDefault("options.island.spawn-limits.golems", 0),
                        root.integerWithDefault("options.island.spawn-limits.copper-golems", 0)
                    ),
                    loadIntMap(root.child("options.island.block-limits"), "enabled")
                ),
                new RuntimeConfig.Extras(
                    root.boolWithDefault("options.extras.sendToSpawn"),
                    root.boolWithDefault("options.extras.respawnAtIsland"),
                    root.boolWithDefault("options.extras.obsidianToLava")
                ),
                new RuntimeConfig.Protection(
                    root.bool("options.protection.enabled", true),
                    root.boolWithDefault("options.protection.item-drops"),
                    root.boolWithDefault("options.protection.visitors.item-drops"),
                    root.boolWithDefault("options.protection.creepers"),
                    root.boolWithDefault("options.protection.withers"),
                    root.boolWithDefault("options.protection.protect-lava"),
                    root.boolWithDefault("options.protection.visitors.trampling"),
                    root.boolWithDefault("options.protection.visitors.kill-animals"),
                    root.boolWithDefault("options.protection.visitors.kill-monsters"),
                    root.boolWithDefault("options.protection.visitors.shearing"),
                    root.boolWithDefault("options.protection.visitors.hatching"),
                    root.boolWithDefault("options.protection.visitors.fall"),
                    root.boolWithDefault("options.protection.visitors.fire-damage"),
                    root.boolWithDefault("options.protection.visitors.monster-damage"),
                    root.boolWithDefault("options.protection.visitors.warn-on-warp"),
                    root.boolWithDefault("options.protection.visitors.villager-trading"),
                    root.boolWithDefault("options.protection.villager-trading-enabled"),
                    root.boolWithDefault("options.protection.visitors.use-portals"),
                    root.boolWithDefault("options.protection.visitors.vehicle-enter"),
                    root.boolWithDefault("options.protection.visitors.vehicle-damage"),
                    root.boolWithDefault("options.protection.visitors.block-banned-entry")
                ),
                new RuntimeConfig.Nether(
                    root.boolWithDefault("nether.enabled"),
                    root.integerWithDefault("nether.lava-level", 0),
                    root.integerWithDefault("nether.height", 0),
                    root.string("nether.chunk-generator", DEFAULT_NETHER_CHUNK_GENERATOR),
                    new RuntimeConfig.Terraform(
                        root.boolWithDefault("nether.terraform-enabled"),
                        root.decimalWithDefault("nether.terraform-min-pitch", -90d, 90d),
                        root.decimalWithDefault("nether.terraform-max-pitch", -90d, 90d),
                        root.integerWithDefault("nether.terraform-distance", 0),
                        loadStringLists(root.child("nether.terraform")),
                        loadDoubleMap(root.child("nether.terraform-weight"), 1d)
                    ),
                    new RuntimeConfig.SpawnChances(
                        root.boolWithDefault("nether.spawn-chances.enabled"),
                        root.decimalWithDefault("nether.spawn-chances.blaze", 0d, 1d),
                        root.decimalWithDefault("nether.spawn-chances.wither", 0d, 1d),
                        root.decimalWithDefault("nether.spawn-chances.skeleton", 0d, 1d)
                    )
                ),
                new RuntimeConfig.Restart(
                    root.boolWithDefault("options.restart.clearInventory"),
                    root.boolWithDefault("options.restart.clearPerms"),
                    root.boolWithDefault("options.restart.clearArmor"),
                    root.boolWithDefault("options.restart.clearEnderChest"),
                    root.boolWithDefault("options.restart.clearCurrency"),
                    root.boolWithDefault("options.restart.teleportWhenReady"),
                    root.durationWithDefault("options.restart.teleportDelay"),
                    root.stringList("options.restart.extra-commands")
                ),
                new RuntimeConfig.Advanced(
                    root.durationWithDefault("options.advanced.confirmTimeout"),
                    root.boolWithDefault("options.advanced.useDisplayNames"),
                root.decimal("options.advanced.topTenCutoff", root.decimalWithDefault("options.advanced.purgeLevel", 0d), 0d),
                    root.boolWithDefault("options.advanced.manageSpawn"),
                root.string("options.advanced.playerCache", DEFAULT_PLAYER_CACHE_SPEC),
                root.string("options.advanced.islandCache", DEFAULT_ISLAND_CACHE_SPEC),
                root.string("options.advanced.placeholderCache", DEFAULT_PLACEHOLDER_CACHE_SPEC),
                root.string("options.advanced.completionCache", DEFAULT_COMPLETION_CACHE_SPEC),
                root.duration("options.advanced.island.saveEvery", DEFAULT_ISLAND_SAVE_EVERY),
                root.duration("options.advanced.player.saveEvery", DEFAULT_PLAYER_SAVE_EVERY),
                root.string("options.advanced.chunk-generator", DEFAULT_OVERWORLD_CHUNK_GENERATOR),
                root.integerWithDefault("options.advanced.chunkRegenSpeed", 0),
                root.duration("async.long.feedbackEvery", DEFAULT_LONG_FEEDBACK_EVERY),
                root.decimalWithDefault("options.advanced.purgeLevel", 0d),
                root.duration("options.advanced.purgeTimeout", DEFAULT_PURGE_TIMEOUT),
                normalizeBlank(root.stringOrNull("options.advanced.debugLevel")),
                new RuntimeConfig.PlayerDb(
                    root.stringWithDefault("options.advanced.playerdb.storage"),
                    root.string("options.advanced.playerdb.nameCache", DEFAULT_PLAYERDB_NAME_CACHE_SPEC),
                    root.string("options.advanced.playerdb.uuidCache", DEFAULT_PLAYERDB_UUID_CACHE_SPEC),
                    root.duration("playerdb.saveDelay", DEFAULT_PLAYERDB_SAVE_DELAY)
                )
            ),
            new RuntimeConfig.Async(
                root.duration("async.maxMs", DEFAULT_ASYNC_MAX_ITERATION_TIME),
                root.longValue("async.maxConsecutiveTicks", DEFAULT_ASYNC_MAX_CONSECUTIVE_TICKS),
                root.duration("async.yieldDelay", DEFAULT_ASYNC_YIELD_DELAY)
            ),
                new RuntimeConfig.AsyncWorldEdit(
                    root.boolWithDefault("asyncworldedit.enabled"),
                    root.durationWithDefault("asyncworldedit.watchDog.heartBeat"),
                    root.durationWithDefault("asyncworldedit.watchDog.timeout")
                ),
                new RuntimeConfig.Party(
                    root.durationWithDefault("options.party.invite-timeout"),
                    root.stringWithDefault("options.party.chat-format"),
                    root.stringList("options.party.join-commands"),
                    root.stringList("options.party.leave-commands"),
                    loadPartyPermissionOverrides(root.child("options.party.maxPartyPermissions"))
                ),
                new RuntimeConfig.PluginUpdates(
                    root.boolWithDefault("plugin-updates.check"),
                    pluginUpdateBranch
                ),
                new RuntimeConfig.Spawning(
                    new RuntimeConfig.Guardians(
                        root.boolWithDefault("options.spawning.guardians.enabled"),
                        root.integerWithDefault("options.spawning.guardians.max-per-island", 0),
                        guardianSpawnChance
                    ),
                    new RuntimeConfig.Phantoms(
                        root.boolWithDefault("options.spawning.phantoms.overworld"),
                        root.boolWithDefault("options.spawning.phantoms.nether")
                    )
                ),
                new RuntimeConfig.Placeholder(
                    root.boolWithDefault("placeholder.chatplaceholder"),
                    root.boolWithDefault("placeholder.servercommandplaceholder"),
                    root.boolWithDefault("placeholder.mvdwplaceholderapi")
                ),
                new RuntimeConfig.ToolMenu(
                    root.boolWithDefault("tool-menu.enabled"),
                    gameObjects.itemStack(root.stringWithDefault("tool-menu.tool")),
                    loadToolMenuCommands(root.child("tool-menu.commands"))
                ),
            new RuntimeConfig.Signs(root.boolWithDefault("signs.enabled")),
            new RuntimeConfig.WorldGuard(
                root.boolWithDefault("worldguard.entry-message"),
                root.boolWithDefault("worldguard.exit-message")
            ),
            new RuntimeConfig.Importer(
                root.decimal("importer.progressEveryFraction", DEFAULT_IMPORTER_PROGRESS_EVERY_FRACTION, 0d, 1d),
                root.duration("importer.progressEveryMs", DEFAULT_IMPORTER_PROGRESS_EVERY)
            ),
            loadIslandSchemes(root),
            loadExtraMenus(root.child("options.extra-menus")),
            loadDonorPerks(root.child("donor-perks")),
            loadConfirmations(root.child("confirmation"))
        );
    }

    @NotNull
    private static Locale loadLocale(@NotNull String configuredLanguage) {
        Locale configured = I18nUtil.getLocale(configuredLanguage);
        return configured != null ? configured : Locale.ENGLISH;
    }

    @NotNull
    private String resolveConfiguredLanguage(@Nullable String configuredLanguage) {
        if (configuredLanguage == null || configuredLanguage.isBlank()) {
            return "en";
        }
        return I18nUtil.findSupportedLocaleKey(configuredLanguage)
            .orElseGet(() -> {
                logger.warning("Config value 'language' references unsupported language '" + configuredLanguage
                    + "'. Using fallback 'en'.");
                return "en";
            });
    }

    private void warnIfNormalized(@NotNull String path, int configuredValue, int actualValue) {
        if (configuredValue != actualValue) {
            logger.warning("Config value '" + path + "' was normalized from " + configuredValue + " to " + actualValue + ".");
        }
    }

    private void warnIfInvalidBiomeKey(@NotNull String path, String biomeKey) {
        if (biomeKey == null) {
            return;
        }
        try {
            if (Registry.BIOME.match(biomeKey) != null) {
                return;
            }
            logger.warning("Config value '" + path + "' references unknown biome '" + biomeKey + "'.");
        } catch (Throwable ignored) {
            // Biome validation is best-effort: config loading must not depend on a live registry
            // in unit tests or during very early bootstrap.
        }
    }

    private void warnIfInvalidUpdateBranch(String branch) {
        if (branch == null || VALID_UPDATE_BRANCHES.contains(branch.toUpperCase(Locale.ROOT))) {
            return;
        }
        logger.warning("Config value 'plugin-updates.branch' has unsupported value '" + branch + "'.");
    }

    @NotNull
    private String normalizeUpdateBranch(@NotNull String branch) {
        if (VALID_UPDATE_BRANCHES.contains(branch.toUpperCase(Locale.ROOT))) {
            return branch;
        }
        warnIfInvalidUpdateBranch(branch);
        return "RELEASE";
    }

    @NotNull
    private static Map<String, List<String>> loadStringLists(@NotNull RuntimeConfigNode section) {
        if (!section.exists()) {
            return Collections.emptyMap();
        }

        Map<String, List<String>> values = new LinkedHashMap<>();
        for (String key : section.keys()) {
            values.put(key, section.stringList(key));
        }
        return Collections.unmodifiableMap(values);
    }

    @NotNull
    private Map<String, List<ItemStackAmountSpec>> loadItemStackAmountLists(@NotNull RuntimeConfigNode section) {
        if (!section.exists()) {
            return Collections.emptyMap();
        }

        Map<String, List<ItemStackAmountSpec>> values = new LinkedHashMap<>();
        for (String key : section.keys()) {
            values.put(key, gameObjects.itemStackAmounts(section.stringList(key)));
        }
        return Collections.unmodifiableMap(values);
    }

    @NotNull
    private static Map<String, Integer> loadIntMap(@NotNull RuntimeConfigNode section, String... ignoredKeys) {
        if (!section.exists()) {
            return Collections.emptyMap();
        }

        LinkedHashSet<String> ignored = new LinkedHashSet<>(List.of(ignoredKeys));
        Map<String, Integer> values = new LinkedHashMap<>();
        for (String key : section.keys()) {
            if (ignored.contains(key)) {
                continue;
            }
            values.put(key, section.integer(key, 0));
        }
        return Collections.unmodifiableMap(values);
    }

    @NotNull
    private List<RuntimeConfig.ToolMenuCommand> loadToolMenuCommands(@NotNull RuntimeConfigNode section) {
        if (!section.exists()) {
            return Collections.emptyList();
        }

        return section.keys().stream()
            .map(key -> new RuntimeConfig.ToolMenuCommand(gameObjects.itemStack(key), section.string(key, "")))
            .filter(command -> command.command() != null && !command.command().isBlank())
            .toList();
    }

    @NotNull
    private static Map<String, Double> loadDoubleMap(@NotNull RuntimeConfigNode section, double defaultValue) {
        if (!section.exists()) {
            return Collections.emptyMap();
        }

        Map<String, Double> values = new LinkedHashMap<>();
        for (String key : section.keys()) {
            values.put(key, section.decimal(key, defaultValue));
        }
        return Collections.unmodifiableMap(values);
    }

    private static boolean isAllowPvP(@NotNull RuntimeConfigNode root) {
        String value = root.stringWithDefault("options.island.allowPvP");
        return "allow".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);
    }

    @NotNull
    private Map<String, RuntimeConfig.IslandScheme> loadIslandSchemes(@NotNull RuntimeConfigNode root) {
        RuntimeConfigNode section = root.child("island-schemes");
        if (!section.exists()) {
            return Collections.emptyMap();
        }

        Map<String, RuntimeConfig.IslandScheme> schemes = new LinkedHashMap<>();
        for (String schemeName : section.keys()) {
            RuntimeConfigNode schemeSection = section.child(schemeName);
            schemes.put(schemeName, new RuntimeConfig.IslandScheme(
                schemeSection.bool("enabled", true),
                normalizeBlank(schemeSection.stringOrNull("schematic")),
                normalizeBlank(schemeSection.stringOrNull("nether-schematic")),
                schemeSection.string("permission", "usb.schematic." + schemeName),
                normalizeBlank(schemeSection.stringOrNull("description")),
                gameObjects.itemStack(schemeSection.string("displayItem", "OAK_SAPLING")),
                    schemeSection.integer("index", 1, 1),
                gameObjects.itemStackAmounts(schemeSection.stringList("extraItems")),
                schemeSection.integer("maxPartySize", 0),
                schemeSection.integer("animals", 0),
                schemeSection.integer("monsters", 0),
                schemeSection.integer("villagers", 0),
                schemeSection.integer("golems", 0),
                schemeSection.integer("copper-golems", 0),
                schemeSection.decimal("scoreMultiply", 1d),
                schemeSection.decimal("scoreOffset", 0d)
            ));
        }
        return Collections.unmodifiableMap(schemes);
    }

    @NotNull
    private Map<Integer, RuntimeConfig.ExtraMenu> loadExtraMenus(@NotNull RuntimeConfigNode section) {
        if (!section.exists()) {
            return Collections.emptyMap();
        }

        Map<Integer, RuntimeConfig.ExtraMenu> menus = new LinkedHashMap<>();
        for (String key : section.keys()) {
            RuntimeConfigNode menuSection = section.child(key);
            try {
                int index = Integer.parseInt(key, 10);
                menus.put(index, new RuntimeConfig.ExtraMenu(
                    menuSection.string("title", "\u00a9Unknown"),
                    gameObjects.itemStack(menuSection.string("displayItem", "CHEST")),
                    menuSection.stringList("lore"),
                    menuSection.stringList("commands")
                ));
            } catch (NumberFormatException ignored) {
            }
        }
        return Collections.unmodifiableMap(menus);
    }

    @NotNull
    private Map<String, RuntimeConfig.PerkSpec> loadDonorPerks(@NotNull RuntimeConfigNode section) {
        if (!section.exists()) {
            return Collections.emptyMap();
        }

        Map<String, RuntimeConfig.PerkSpec> perks = new LinkedHashMap<>();
        loadDonorPerks(section, null, perks);
        return Collections.unmodifiableMap(perks);
    }

    private void loadDonorPerks(RuntimeConfigNode section, String permission, Map<String, RuntimeConfig.PerkSpec> perks) {
        if (isLeafSection(section)) {
            perks.put(permission, new RuntimeConfig.PerkSpec(
                gameObjects.itemStackAmounts(section.stringList("extraItems")),
                section.integer("maxPartySize", 0),
                section.integer("animals", 0),
                section.integer("monsters", 0),
                section.integer("villagers", 0),
                section.integer("golems", 0),
                section.integer("copper-golems", 0),
                section.decimal("rewardBonus", 0d, 0d, 1d),
                section.decimal("hungerReduction", 0d, 0d, 1d),
                section.stringList("schematics")
            ));
            return;
        }

        for (String key : section.keys()) {
            if (!section.isSection(key)) {
                continue;
            }
            loadDonorPerks(section.child(key), permission != null ? permission + "." + key : key, perks);
        }
    }

    private static boolean isLeafSection(RuntimeConfigNode section) {
        for (String key : section.keys()) {
            if (!section.isSection(key)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    private static Map<String, Integer> loadPartyPermissionOverrides(@NotNull RuntimeConfigNode section) {
        Map<String, Integer> values = new LinkedHashMap<>();
        if (section.exists()) {
            loadPartyPermissionOverrides(section, null, values);
        }
        return Collections.unmodifiableMap(values);
    }

    private static void loadPartyPermissionOverrides(RuntimeConfigNode section, String permission, Map<String, Integer> values) {
        for (String key : section.keys()) {
            if (section.isSection(key)) {
                loadPartyPermissionOverrides(section.child(key), permission != null ? permission + "." + key : key, values);
            } else if (permission != null) {
                values.put(permission, section.integer(key, 0));
            }
        }
    }

    @NotNull
    private static Map<String, Boolean> loadConfirmations(@NotNull RuntimeConfigNode section) {
        if (!section.exists()) {
            return Collections.emptyMap();
        }

        Map<String, Boolean> confirmations = new LinkedHashMap<>();
        for (String key : section.keys()) {
            confirmations.put(key, section.bool(key, true));
        }
        return Collections.unmodifiableMap(confirmations);
    }

    private static String normalizeBlank(String value) {
        return value != null && !value.trim().isEmpty() ? value : null;
    }

}
