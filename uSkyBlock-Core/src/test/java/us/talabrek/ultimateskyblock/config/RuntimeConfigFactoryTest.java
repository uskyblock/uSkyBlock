package us.talabrek.ultimateskyblock.config;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigFactory;

import java.time.Duration;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RuntimeConfigFactoryTest {
    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        BukkitServerMock.setupServerMock();
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
        config.set("options.island.schematicName", "default");
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
        config.set("nether.lava_level", 11);
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

        RuntimeConfig runtimeConfig = RuntimeConfigFactory.load(config);

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

        RuntimeConfig runtimeConfig = RuntimeConfigFactory.load(config);

        assertEquals(Duration.ofSeconds(2), runtimeConfig.general().maxSpam());
        assertFalse(runtimeConfig.protection().visitorVehicleBreakAllowed());
    }

    @Test
    public void fallsBackToEnglishForInvalidLanguage() {
        YamlConfiguration config = new YamlConfiguration();
        config.setDefaults(PluginConfigLoader.loadBundledConfig());
        config.set("language", "definitely-not-a-real-locale");

        RuntimeConfig runtimeConfig = RuntimeConfigFactory.load(config);

        assertEquals("en", runtimeConfig.configuredLanguage());
        assertEquals(Locale.ENGLISH, runtimeConfig.locale());
    }

    @Test
    public void usesCodeDefaultsForHiddenExpertOnlyKeysMissingFromTemplate() {
        YamlConfiguration config = new YamlConfiguration();
        config.setDefaults(PluginConfigLoader.loadBundledConfig());

        RuntimeConfig runtimeConfig = RuntimeConfigFactory.load(config);

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
        assertEquals(10d, runtimeConfig.importer().progressEveryPct());
        assertEquals(Duration.ofSeconds(10), runtimeConfig.importer().progressEvery());
    }
}
