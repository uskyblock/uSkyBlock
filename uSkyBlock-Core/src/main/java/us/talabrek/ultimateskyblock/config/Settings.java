package us.talabrek.ultimateskyblock.config;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import us.talabrek.ultimateskyblock.handler.WorldEditHandler;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Settings {
    private static final Logger log = Logger.getLogger(Settings.class.getName());
    public static int general_maxPartySize;
    public static String general_worldName;
    public static int island_distance;
    public static int island_height;
    public static int general_spawnSize;
    public static boolean island_removeCreaturesByTeleport;
    public static int island_protectionRange;
    public static int island_radius;
    private static List<ItemStack> island_chestItems;
    public static boolean island_addExtraItems;
    public static String[] island_extraPermissions;
    public static boolean island_allowIslandLock;
    public static boolean island_useIslandLevel;
    public static boolean island_useTopTen;
    public static int general_cooldownInfo;
    public static Duration general_cooldownRestart;
    public static Duration general_biomeChange;
    public static Biome general_defaultBiome;
    public static Biome general_defaultNetherBiome;
    public static boolean extras_sendToSpawn;
    public static boolean extras_respawnAtIsland;
    public static boolean extras_obsidianToLava;
    public static String island_schematicName;
    public static Duration island_topTenTimeout;
    public static boolean island_allowPvP;
    public static Locale locale = Locale.getDefault();
    public static boolean nether_enabled;
    public static int nether_lava_level;
    public static int nether_height;

    public static boolean loadPluginConfig(FileConfiguration config) {
        boolean changed = false;
        try {
            general_maxPartySize = config.getInt("options.general.maxPartySize");
            if (general_maxPartySize < 0) {
                general_maxPartySize = 0;
            }
        } catch (Exception e) {
            general_maxPartySize = 4;
        }
        try {
            island_distance = config.getInt("options.island.distance");
            if (island_distance < 50) {
                island_distance = 50;
            }
        } catch (Exception e) {
            island_distance = 110;
        }
        try {
            island_protectionRange = config.getInt("options.island.protectionRange");
            if (island_protectionRange > island_distance) {
                island_protectionRange = island_distance;
            }
        } catch (Exception e) {
            island_protectionRange = 128;
        }
        island_radius = island_protectionRange / 2;
        try {
            general_cooldownInfo = config.getInt("options.general.cooldownInfo");
            if (general_cooldownInfo < 0) {
                general_cooldownInfo = 0;
            }
        } catch (Exception e) {
            general_cooldownInfo = 60;
        }
        try {
            general_biomeChange = ConfigDuration.parse(config.getString("options.general.biomeChange"));
            if (general_biomeChange.isNegative()) {
                general_biomeChange = Duration.ZERO;
            }
        } catch (Exception e) {
            general_biomeChange = Duration.ofHours(1);
        }
        general_defaultBiome = loadBiome(config, "options.general.defaultBiome", Biome.OCEAN);
        general_defaultNetherBiome = loadBiome(config, "options.general.defaultNetherBiome", Biome.NETHER_WASTES);

        try {
            general_cooldownRestart = ConfigDuration.parse(config.getString("options.general.cooldownRestart"));
            if (general_cooldownRestart.isNegative()) {
                general_cooldownRestart = Duration.ZERO;
            }
        } catch (Exception e) {
            general_cooldownRestart = Duration.ofHours(1);
        }
        try {
            island_height = config.getInt("options.island.height");
            if (island_height < 20) {
                island_height = 20;
            }
        } catch (Exception e) {
            island_height = 120;
        }
        general_spawnSize = config.getInt("options.general.spawnSize", 50);
        island_chestItems = ItemStackUtil.createItemList(config.getStringList("options.island.chestItems"));

        island_schematicName = PluginConfigLoader.normalizeIslandSchematicName(
            config.getString("options.island.schematicName", "default"));
        final Set<String> permissionList = new HashSet<>();
        if (config.isConfigurationSection("options.island.extraPermissions")) {
            permissionList.addAll(config.getConfigurationSection("options.island.extraPermissions").getKeys(false));
        }
        island_addExtraItems = config.getBoolean("options.island.addExtraItems");
        extras_obsidianToLava = config.getBoolean("options.extras.obsidianToLava", true);
        island_useIslandLevel = config.getBoolean("options.island.useIslandLevel");
        island_extraPermissions = permissionList.toArray(new String[0]);
        extras_sendToSpawn = config.getBoolean("options.extras.sendToSpawn");
        extras_respawnAtIsland = config.getBoolean("options.extras.respawnAtIsland");
        island_useTopTen = config.getBoolean("options.island.useTopTen");
        general_worldName = config.getString("options.general.worldName", "skyworld");
        island_removeCreaturesByTeleport = config.getBoolean("options.island.removeCreaturesByTeleport");
        island_allowIslandLock = config.getBoolean("options.island.allowIslandLock");
        island_topTenTimeout = ConfigDuration.parse(config.getString("options.island.topTenTimeout", "7m"));
        island_allowPvP = config.getString("options.island.allowPvP", "deny").equalsIgnoreCase("allow") ||
            config.getString("options.island.allowPvP", "false").equalsIgnoreCase("true");
        Locale loc = I18nUtil.getLocale(config.getString("language", null));
        if (loc != null) {
            locale = loc;
        }
        nether_enabled = config.getBoolean("nether.enabled", false);
        if (nether_enabled && !WorldEditHandler.isOuterPossible()) {
            log.warning("Nether DISABLED, since islands cannot be chunk-aligned!");
            nether_enabled = false;
            changed = true;
        }
        nether_lava_level = config.getInt("nether.lava_level", config.getInt("nether.lava-level", 32));
        nether_height = config.getInt("nether.height", island_height / 2);
        return changed;
    }

    private static Biome loadBiome(FileConfiguration config, String path, Biome defaultBiome) {
        try {
            String biomeKey = config.getString(path, defaultBiome.getKey().getKey());
            Biome parsedBiome = Registry.BIOME.match(biomeKey);
            if (parsedBiome == null) {
                log.log(Level.WARNING, "Invalid Biome in '{0}': {1}", new Object[]{path, biomeKey});
                parsedBiome = defaultBiome;
            }
            return parsedBiome;
        } catch (Exception e) {
            return defaultBiome;
        }
    }

    public static List<ItemStack> getIslandChestItems() {
        return ItemStackUtil.clone(island_chestItems);
    }
}
