package us.talabrek.ultimateskyblock.config;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigFactory;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;

import java.time.Duration;
import java.util.Locale;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RuntimeConfigFactoryTest {
    private RuntimeConfigFactory runtimeConfigFactory;
    private TestLogHandler logHandler;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        BukkitServerMock.setupServerMock();
        logHandler = new TestLogHandler();
        runtimeConfigFactory = new RuntimeConfigFactory(new GameObjectFactory(), testLogger(logHandler));
    }

    @Test
    public void loadsTypedRuntimeConfigSections() {
        YamlConfiguration config = new YamlConfiguration();
        config.setDefaults(PluginConfigLoader.loadBundledConfig());
        config.set("language", "en");
        config.set("init.initDelay", "4s");
        config.set("options.general.cooldownInfo", "15s");
        config.set("options.general.maxSpam", "1500ms");
        config.set("plugin-updates.branch", "STAGING");
        config.set("options.general.maxPartySize", 6);
        config.set("options.general.worldName", "skyworld");
        config.set("options.general.cooldownRestart", "30s");
        config.set("options.general.biomeChange", "2m");
        config.set("options.general.defaultBiome", "plains");
        config.set("options.general.defaultNetherBiome", "nether_wastes");
        config.set("options.general.spawnSize", 64);
        config.set("options.island.distance", 120);
        config.set("options.island.height", 150);
        config.set("options.island.removeCreaturesByTeleport", true);
        config.set("options.island.protectionRange", 96);
        config.set("options.island.chestItems", java.util.List.of("ice:2"));
        config.set("options.island.addExtraItems", true);
        config.set("options.island.allowIslandLock", true);
        config.set("options.island.useIslandLevel", true);
        config.set("options.island.useTopTen", true);
        config.set("options.island.default-scheme", "default");
        config.set("options.island.topTenTimeout", "15m");
        config.set("options.island.allowPvP", "allow");
        config.set("options.island.islandTeleportDelay", "5s");
        config.set("options.island.teleportCancelDistance", 0.5d);
        config.set("options.island.autoRefreshScore", "5m");
        config.set("options.island.spawn-limits.enabled", false);
        config.set("options.island.extraPermissions.warp", java.util.List.of("diamond:1"));
        config.set("options.extras.sendToSpawn", true);
        config.set("options.extras.respawnAtIsland", true);
        config.set("options.extras.obsidianToLava", true);
        config.set("options.protection.creepers", false);
        config.set("options.protection.withers", false);
        config.set("options.protection.visitors.trampling", false);
        config.set("options.protection.visitors.kill-animals", false);
        config.set("options.protection.visitors.kill-monsters", false);
        config.set("options.protection.visitors.shearing", false);
        config.set("options.protection.visitors.hatching", false);
        config.set("options.protection.visitors.villager-trading", false);
        config.set("options.protection.villager-trading-enabled", true);
        config.set("options.protection.visitors.use-portals", true);
        config.set("options.protection.visitors.vehicle-enter", true);
        config.set("options.protection.visitors.vehicle-damage", true);
        config.set("options.advanced.confirmTimeout", "12s");
        config.set("options.party.invite-timeout", "3m");
        config.set("options.spawning.guardians.enabled", false);
        config.set("options.spawning.guardians.max-per-island", 4);
        config.set("options.spawning.guardians.spawn-chance", 0.25d);
        config.set("nether.enabled", true);
        config.set("nether.lava-level", 11);
        config.set("nether.height", 80);
        config.set("nether.terraform-enabled", false);
        config.set("nether.terraform-min-pitch", -50d);
        config.set("nether.terraform-max-pitch", 30d);
        config.set("nether.terraform-distance", 9);
        config.set("nether.terraform.COBBLESTONE", java.util.List.of("NETHERRACK:1.0"));
        config.set("nether.terraform-weight.pickaxe", 1.5d);
        config.set("nether.spawn-chances.enabled", false);
        config.set("nether.spawn-chances.blaze", 0.3d);
        config.set("nether.spawn-chances.wither", 0.5d);
        config.set("nether.spawn-chances.skeleton", 0.2d);
        config.set("asyncworldedit.watchDog.heartBeat", "2500ms");
        config.set("island-schemes.default.enabled", true);
        config.set("island-schemes.default.schematic", "default.schematic");
        config.set("island-schemes.default.nether-schematic", "uSkyBlockNether.schem");
        config.set("confirmation.is restart", false);

        RuntimeConfig runtimeConfig = runtimeConfigFactory.load(config);

        assertEquals(6, runtimeConfig.general().maxPartySize());
        assertEquals(Duration.ofSeconds(4), runtimeConfig.init().initDelay());
        assertEquals(Duration.ofSeconds(15), runtimeConfig.general().cooldownInfo());
        assertEquals(Duration.ofSeconds(30), runtimeConfig.general().cooldownRestart());
        assertEquals("plains", runtimeConfig.general().defaultBiomeKey());
        assertEquals(Duration.ofMillis(1500), runtimeConfig.general().maxSpam());
        assertEquals(96, runtimeConfig.island().protectionRange());
        assertEquals(48, runtimeConfig.island().radius());
        assertEquals(Material.ICE, runtimeConfig.island().chestItems().get(0).prototype().create().getType());
        assertEquals(2, runtimeConfig.island().chestItems().get(0).amount());
        assertEquals(Duration.ofSeconds(5), runtimeConfig.island().teleportDelay());
        assertEquals(0.5d, runtimeConfig.island().teleportCancelDistance());
        assertEquals(Duration.ofMinutes(5), runtimeConfig.island().autoRefreshScore());
        assertFalse(runtimeConfig.island().spawnLimits().enabled());
        assertTrue(runtimeConfig.extras().sendToSpawn());
        assertFalse(runtimeConfig.protection().creepers());
        assertFalse(runtimeConfig.protection().withers());
        assertFalse(runtimeConfig.protection().visitorTramplingProtected());
        assertFalse(runtimeConfig.protection().visitorKillAnimalsProtected());
        assertFalse(runtimeConfig.protection().visitorKillMonstersProtected());
        assertFalse(runtimeConfig.protection().visitorShearingProtected());
        assertFalse(runtimeConfig.protection().visitorHatchingProtected());
        assertFalse(runtimeConfig.protection().visitorVillagerTradingProtected());
        assertTrue(runtimeConfig.protection().anyVillagerTradingAllowed());
        assertTrue(runtimeConfig.protection().visitorUsePortalsAllowed());
        assertTrue(runtimeConfig.protection().visitorVehicleEnterAllowed());
        assertTrue(runtimeConfig.protection().visitorVehicleBreakAllowed());
        assertEquals(Duration.ofSeconds(12), runtimeConfig.advanced().confirmTimeout());
        assertEquals(Duration.ofMinutes(3), runtimeConfig.party().inviteTimeout());
        assertEquals("STAGING", runtimeConfig.pluginUpdates().branch());
        assertFalse(runtimeConfig.spawning().guardians().enabled());
        assertEquals(4, runtimeConfig.spawning().guardians().maxPerIsland());
        assertEquals(0.25d, runtimeConfig.spawning().guardians().spawnChance());
        assertTrue(runtimeConfig.nether().enabled());
        assertFalse(runtimeConfig.nether().terraform().enabled());
        assertEquals(9, runtimeConfig.nether().terraform().distance());
        assertEquals(java.util.List.of("NETHERRACK:1.0"), runtimeConfig.nether().terraform().blocks().get("COBBLESTONE"));
        assertEquals(1.5d, runtimeConfig.nether().terraform().toolWeights().get("pickaxe"));
        assertFalse(runtimeConfig.nether().spawnChances().enabled());
        assertEquals(0.3d, runtimeConfig.nether().spawnChances().blaze());
        assertEquals(Duration.ofMillis(2500), runtimeConfig.asyncWorldEdit().heartBeat());
        assertEquals("default.schematic", runtimeConfig.islandScheme("default").schematic());
        assertEquals("uSkyBlockNether.schem", runtimeConfig.islandScheme("default").netherSchematic());
        assertEquals(Material.OAK_SAPLING, runtimeConfig.islandScheme("default").displayItem().create().getType());
        assertFalse(runtimeConfig.confirmationRequired("is restart", true));
    }

    @Test
    public void ignoresLegacyMiskeyedRuntimePathsWithoutMigration() {
        YamlConfiguration config = new YamlConfiguration();
        config.setDefaults(PluginConfigLoader.loadBundledConfig());
        config.set("general.maxSpam", 1700);
        config.set("options.protection.visitors.vehicle-break", true);

        RuntimeConfig runtimeConfig = runtimeConfigFactory.load(config);

        assertEquals(Duration.ofSeconds(2), runtimeConfig.general().maxSpam());
        assertFalse(runtimeConfig.protection().visitorVehicleBreakAllowed());
    }

    @Test
    public void fallsBackToEnglishForInvalidLanguage() {
        YamlConfiguration config = new YamlConfiguration();
        config.setDefaults(PluginConfigLoader.loadBundledConfig());
        config.set("language", "definitely-not-a-real-locale");

        RuntimeConfig runtimeConfig = runtimeConfigFactory.load(config);

        assertEquals("en", runtimeConfig.configuredLanguage());
        assertEquals(Locale.ENGLISH, runtimeConfig.locale());
        assertTrue(logHandler.messages.stream().anyMatch(message -> message.contains("Config value 'language' references unsupported language")));
    }

    @Test
    public void usesCodeDefaultsForHiddenExpertOnlyKeysMissingFromTemplate() {
        YamlConfiguration config = new YamlConfiguration();
        config.setDefaults(PluginConfigLoader.loadBundledConfig());

        RuntimeConfig runtimeConfig = runtimeConfigFactory.load(config);

        assertEquals(Duration.ofMillis(2500), runtimeConfig.init().initDelay());
        assertEquals(Duration.ofSeconds(20), runtimeConfig.general().cooldownInfo());
        assertEquals("maximumSize=200,expireAfterWrite=15m,expireAfterAccess=10m", runtimeConfig.advanced().playerCacheSpec());
        assertEquals("maximumSize=200,expireAfterWrite=15m,expireAfterAccess=10m", runtimeConfig.advanced().islandCacheSpec());
        assertEquals("maximumSize=200,expireAfterWrite=20s", runtimeConfig.advanced().placeholderCacheSpec());
        assertEquals("maximumSize=200,expireAfterWrite=15m,expireAfterAccess=10m", runtimeConfig.advanced().completionCacheSpec());
        assertEquals(Duration.ofSeconds(30), runtimeConfig.advanced().islandSaveEvery());
        assertEquals(Duration.ofSeconds(120), runtimeConfig.advanced().playerSaveEvery());
        assertEquals("us.talabrek.ultimateskyblock.world.SkyBlockChunkGenerator", runtimeConfig.advanced().chunkGenerator());
        assertEquals(Duration.ofSeconds(30), runtimeConfig.advanced().feedbackEvery());
        assertEquals(Duration.ofMinutes(10), runtimeConfig.advanced().purgeTimeout());
        assertEquals(Duration.ofSeconds(2), runtimeConfig.general().maxSpam());
        assertEquals(Duration.ZERO, runtimeConfig.island().autoRefreshScore());
        assertEquals(Duration.ofSeconds(2), runtimeConfig.asyncWorldEdit().heartBeat());
        assertEquals("us.talabrek.ultimateskyblock.world.SkyBlockNetherChunkGenerator", runtimeConfig.nether().chunkGenerator());
        assertEquals("maximumSize=1500,expireAfterWrite=30m,expireAfterAccess=15m", runtimeConfig.advanced().playerDb().nameCacheSpec());
        assertEquals("maximumSize=1500,expireAfterWrite=30m,expireAfterAccess=15m", runtimeConfig.advanced().playerDb().uuidCacheSpec());
        assertEquals(Duration.ofSeconds(10), runtimeConfig.advanced().playerDb().saveDelay());
        assertEquals(Duration.ofMillis(15), runtimeConfig.async().maxIterationTime());
        assertEquals(20L, runtimeConfig.async().maxConsecutiveTicks());
        assertEquals(Duration.ofMillis(100), runtimeConfig.async().yieldDelay());
        assertEquals(0.10d, runtimeConfig.importer().progressEveryFraction());
        assertEquals(Duration.ofSeconds(10), runtimeConfig.importer().progressEvery());
    }

    @Test
    public void warnsForInvalidBranchAndNormalizedValues() {
        YamlConfiguration config = new YamlConfiguration();
        config.setDefaults(PluginConfigLoader.loadBundledConfig());
        config.set("options.general.defaultBiome", "definitely_not_a_biome");
        config.set("options.general.defaultNetherBiome", "also_not_a_biome");
        config.set("plugin-updates.branch", "BETA");
        config.set("options.general.maxPartySize", -1);
        config.set("options.island.distance", 10);
        config.set("options.island.protectionRange", 999);
        config.set("options.island.height", 5);
        config.set("options.island.log-size", -2);
        config.set("options.spawning.guardians.max-per-island", -3);
        config.set("options.spawning.guardians.spawn-chance", 2.5d);
        config.set("options.island.teleportCancelDistance", -0.5d);
        config.set("options.advanced.chunkRegenSpeed", -4);
        config.set("options.advanced.purgeLevel", -1d);
        config.set("options.advanced.topTenCutoff", -2d);
        config.set("nether.terraform-distance", -5);
        config.set("nether.terraform-min-pitch", -120d);
        config.set("nether.terraform-max-pitch", 120d);
        config.set("nether.spawn-chances.blaze", 2d);
        config.set("nether.spawn-chances.wither", -1d);
        config.set("donor-perks.usb.test.rewardBonus", 2d);
        config.set("donor-perks.usb.test.hungerReduction", -1d);
        config.set("options.general.maxSpam", "-10s");
        config.set("options.party.invite-timeout", "-1s");
        config.set("island-schemes.default.index", 0);
        config.set("confirmation", false);
        config.set("importer.progressEveryFraction", 2.5d);

        RuntimeConfig runtimeConfig = runtimeConfigFactory.load(config);

        assertTrue(logHandler.messages.stream().anyMatch(message -> message.contains("plugin-updates.branch")));
        assertTrue(logHandler.messages.stream().anyMatch(message -> message.contains("options.general.maxPartySize")));
        assertTrue(logHandler.messages.stream().anyMatch(message -> message.contains("options.island.distance")));
        assertTrue(logHandler.messages.stream().anyMatch(message -> message.contains("options.island.protectionRange")));
        assertTrue(logHandler.messages.stream().anyMatch(message -> message.contains("options.island.height")));
        assertTrue(logHandler.messages.stream().anyMatch(message -> message.contains("options.island.log-size")));
        assertTrue(logHandler.messages.stream().anyMatch(message -> message.contains("options.spawning.guardians.spawn-chance")));
        assertTrue(logHandler.messages.stream().anyMatch(message -> message.contains("options.spawning.guardians.max-per-island")));
        assertTrue(logHandler.messages.stream().anyMatch(message -> message.contains("options.island.teleportCancelDistance")));
        assertTrue(logHandler.messages.stream().anyMatch(message -> message.contains("options.advanced.chunkRegenSpeed")));
        assertTrue(logHandler.messages.stream().anyMatch(message -> message.contains("options.advanced.purgeLevel")));
        assertTrue(logHandler.messages.stream().anyMatch(message -> message.contains("options.advanced.topTenCutoff")));
        assertTrue(logHandler.messages.stream().anyMatch(message -> message.contains("nether.terraform-distance")));
        assertTrue(logHandler.messages.stream().anyMatch(message -> message.contains("nether.terraform-min-pitch")));
        assertTrue(logHandler.messages.stream().anyMatch(message -> message.contains("nether.terraform-max-pitch")));
        assertTrue(logHandler.messages.stream().anyMatch(message -> message.contains("nether.spawn-chances.blaze")));
        assertTrue(logHandler.messages.stream().anyMatch(message -> message.contains("nether.spawn-chances.wither")));
        assertTrue(logHandler.messages.stream().anyMatch(message -> message.contains("options.general.maxSpam")));
        assertTrue(logHandler.messages.stream().anyMatch(message -> message.contains("options.party.invite-timeout")));
        assertTrue(logHandler.messages.stream().anyMatch(message -> message.contains("island-schemes.default.index")));
        assertTrue(logHandler.messages.stream().anyMatch(message -> message.contains("confirmation")));
        assertTrue(logHandler.messages.stream().anyMatch(message -> message.contains("importer.progressEveryFraction")));
        assertEquals(0, runtimeConfig.general().maxPartySize());
        assertEquals(Duration.ZERO, runtimeConfig.general().maxSpam());
        assertEquals(0d, runtimeConfig.island().teleportCancelDistance());
        assertEquals(0, runtimeConfig.island().logSize());
        assertEquals(0, runtimeConfig.spawning().guardians().maxPerIsland());
        assertEquals(1.0d, runtimeConfig.spawning().guardians().spawnChance());
        assertEquals(0d, runtimeConfig.advanced().purgeLevel());
        assertEquals(0d, runtimeConfig.advanced().topTenCutoff());
        assertEquals(1.0d, runtimeConfig.importer().progressEveryFraction());
        assertEquals(0, runtimeConfig.nether().terraform().distance());
        assertEquals(-90d, runtimeConfig.nether().terraform().minPitch());
        assertEquals(90d, runtimeConfig.nether().terraform().maxPitch());
        assertEquals(1d, runtimeConfig.nether().spawnChances().blaze());
        assertEquals(0d, runtimeConfig.nether().spawnChances().wither());
        assertEquals(0, runtimeConfig.advanced().chunkRegenSpeed());
        assertEquals(Duration.ZERO, runtimeConfig.party().inviteTimeout());
        assertEquals(1, runtimeConfig.islandScheme("default").index());
        assertEquals("RELEASE", runtimeConfig.pluginUpdates().branch());
        assertEquals(1d, runtimeConfig.donorPerks().get("usb.test").rewardBonus());
        assertEquals(0d, runtimeConfig.donorPerks().get("usb.test").hungerReduction());
    }

    @Test
    public void fallsBackForInvalidItemSpecifications() {
        YamlConfiguration config = new YamlConfiguration();
        config.setDefaults(PluginConfigLoader.loadBundledConfig());
        config.set("options.island.chestItems", java.util.List.of("definitely_not_a_real_item:1"));
        config.set("tool-menu.tool", "also_not_a_real_item");
        config.set("tool-menu.commands.NOT_A_REAL_ITEM", "challenges");
        config.set("options.island.extraPermissions.custom", java.util.List.of("bad_item:1"));

        RuntimeConfig runtimeConfig = runtimeConfigFactory.load(config);

        assertEquals(Material.ICE, runtimeConfig.island().chestItems().get(0).prototype().create().getType());
        assertEquals(Material.OAK_SAPLING, runtimeConfig.toolMenu().tool().create().getType());
        assertEquals(3, runtimeConfig.toolMenu().commands().size());
        assertEquals(java.util.List.of(), runtimeConfig.island().extraPermissionItems().get("custom"));
        assertTrue(logHandler.messages.stream().anyMatch(message -> message.contains("options.island.chestItems")));
        assertTrue(logHandler.messages.stream().anyMatch(message -> message.contains("tool-menu.tool")));
        assertTrue(logHandler.messages.stream().anyMatch(message -> message.contains("tool-menu.commands.NOT_A_REAL_ITEM")));
        assertTrue(logHandler.messages.stream().anyMatch(message -> message.contains("options.island.extraPermissions.custom")));
    }

    @Test
    public void ignoresBiomeValidationWhenRegistryIsUnavailable() {
        YamlConfiguration config = new YamlConfiguration();
        config.setDefaults(PluginConfigLoader.loadBundledConfig());
        config.set("options.general.defaultBiome", "definitely_not_a_biome");
        config.set("options.general.defaultNetherBiome", "also_not_a_biome");

        RuntimeConfig runtimeConfig = runtimeConfigFactory.load(config);

        assertEquals("definitely_not_a_biome", runtimeConfig.general().defaultBiomeKey());
        assertEquals("also_not_a_biome", runtimeConfig.general().defaultNetherBiomeKey());
    }

    private static Logger testLogger(TestLogHandler handler) {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        logger.addHandler(handler);
        return logger;
    }

    private static final class TestLogHandler extends Handler {
        private final java.util.List<String> messages = new java.util.ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            messages.add(record.getMessage());
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
