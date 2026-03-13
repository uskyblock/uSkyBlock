package us.talabrek.ultimateskyblock.config;

import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import org.bukkit.block.Biome;
import org.bukkit.Registry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigFactory;
import us.talabrek.ultimateskyblock.handler.WorldEditHandler;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
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
        RuntimeConfig runtimeConfig = RuntimeConfigFactory.load(config);
        boolean changed = false;
        general_maxPartySize = runtimeConfig.general().maxPartySize();
        island_distance = runtimeConfig.island().distance();
        island_protectionRange = runtimeConfig.island().protectionRange();
        island_radius = island_protectionRange / 2;
        general_cooldownInfo = runtimeConfig.general().cooldownInfo();
        general_biomeChange = runtimeConfig.general().biomeChange();
        general_defaultBiome = loadBiome("options.general.defaultBiome", runtimeConfig.general().defaultBiomeKey(), Biome.OCEAN);
        general_defaultNetherBiome = loadBiome("options.general.defaultNetherBiome", runtimeConfig.general().defaultNetherBiomeKey(), Biome.NETHER_WASTES);
        general_cooldownRestart = runtimeConfig.general().cooldownRestart();
        island_height = runtimeConfig.island().height();
        general_spawnSize = runtimeConfig.general().spawnSize();
        island_chestItems = ItemStackUtil.createItemList(runtimeConfig.island().chestItemSpecs());
        island_schematicName = runtimeConfig.island().defaultScheme();
        island_addExtraItems = runtimeConfig.island().addExtraItems();
        extras_obsidianToLava = runtimeConfig.extras().obsidianToLava();
        island_useIslandLevel = runtimeConfig.island().useIslandLevel();
        island_extraPermissions = runtimeConfig.island().extraPermissions().toArray(new String[0]);
        extras_sendToSpawn = runtimeConfig.extras().sendToSpawn();
        extras_respawnAtIsland = runtimeConfig.extras().respawnAtIsland();
        island_useTopTen = runtimeConfig.island().useTopTen();
        general_worldName = runtimeConfig.general().worldName();
        island_removeCreaturesByTeleport = runtimeConfig.island().removeCreaturesByTeleport();
        island_allowIslandLock = runtimeConfig.island().allowIslandLock();
        island_topTenTimeout = runtimeConfig.island().topTenTimeout();
        island_allowPvP = runtimeConfig.island().allowPvP();
        locale = runtimeConfig.locale();
        nether_enabled = runtimeConfig.nether().enabled();
        if (nether_enabled && !WorldEditHandler.isOuterPossible()) {
            log.warning("Nether DISABLED, since islands cannot be chunk-aligned!");
            nether_enabled = false;
            changed = true;
        }
        nether_lava_level = runtimeConfig.nether().lavaLevel();
        nether_height = runtimeConfig.nether().height();
        return changed;
    }

    public static List<ItemStack> getIslandChestItems() {
        return island_chestItems.stream().map(ItemStack::clone).toList();
    }

    private static Biome loadBiome(String path, String biomeKey, Biome defaultBiome) {
        try {
            Biome parsedBiome = Registry.BIOME.match(biomeKey);
            if (parsedBiome != null) {
                return parsedBiome;
            }
            log.log(Level.WARNING, "Invalid Biome in '{0}': {1}", new Object[]{path, biomeKey});
            return defaultBiome;
        } catch (Exception e) {
            return defaultBiome;
        }
    }
}
