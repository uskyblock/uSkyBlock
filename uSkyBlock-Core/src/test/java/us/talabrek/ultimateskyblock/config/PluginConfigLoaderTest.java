package us.talabrek.ultimateskyblock.config;

import dk.lockfuglsang.minecraft.file.FileUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import us.talabrek.ultimateskyblock.config.migration.PluginConfigMigrator;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class PluginConfigLoaderTest {
    private static final List<String> RESTART_COOLDOWN_COMMENT = List.of(
        "# [duration] The time before a player can use the /island restart command again. Use ms, s, m, h, or d.");
    private static final List<String> INVITE_TIMEOUT_COMMENT = List.of(
        "# [duration] How long an island invite stays valid. Use ms, s, m, h, or d.");
    private static final List<String> RESTART_TELEPORT_COMMENT = List.of(
        "# [duration] The time to wait before porting the player back on /is restart or /is create. Use ms, s, m, h, or d.");
    private static final List<String> GUARDIAN_ENABLED_COMMENT = List.of(
        "# [true/false] If true, deep-ocean prismarine habitats can replace water-mob spawns with guardians.");
    private static final List<String> GUARDIAN_CAP_COMMENT = List.of(
        "# [integer] Maximum number of guardians allowed at one island guardian habitat. This safety cap always applies.");
    private static final List<String> GUARDIAN_CHANCE_COMMENT = List.of(
        "# [number] Chance from 0.0 to 1.0 that an eligible water-mob spawn is replaced with a guardian.");
    @TempDir
    Path tempDir;

    @Test
    public void upgradesLegacyConfigThroughTheCurrentCompatibilityPath() throws Exception {
        FileUtil.setDataFolder(tempDir.toFile());
        File configFile = tempDir.resolve("config.yml").toFile();
        try (var reader = Objects.requireNonNull(
            getClass().getResourceAsStream("/us/talabrek/ultimateskyblock/imports/old-config.yml"))) {
            Files.copy(reader, configFile.toPath());
        }

        PluginConfigLoader loader = new PluginConfigLoader(tempDir, new PluginConfigMigrator(Logger.getAnonymousLogger()));
        YamlConfiguration config = loader.load();

        assertEquals(loadBundledVersion(), config.getInt("version"));
        assertEquals("default", config.getString("options.island.schematicName"));
        assertTrue(config.getBoolean("options.extras.obsidianToLava"));
        assertEquals("30s", config.getString("options.general.cooldownRestart"));
        assertEquals("60s", config.getString("options.general.biomeChange"));
        assertEquals("2s", config.getString("options.island.islandTeleportDelay"));
        assertEquals("20m", config.getString("options.island.topTenTimeout"));
        assertEquals("30s", config.getString("options.party.invite-timeout"));
        assertEquals("10s", config.getString("options.advanced.confirmTimeout"));
        assertEquals("1000ms", config.getString("options.restart.teleportDelay"));
        assertTrue(config.getBoolean("options.spawning.guardians.enabled"));
        assertEquals(10, config.getInt("options.spawning.guardians.max-per-island"));
        assertEquals(0.1d, config.getDouble("options.spawning.guardians.spawn-chance"), 0.00001d);
        assertFalse(config.contains("force-replace"));
        assertFalse(config.contains("move-nodes"));
        assertFalse(config.contains("options.deprecated.fixFlatland"));
        assertTrue(tempDir.resolve("config.yml.old").toFile().isFile());
        try (var stream = Files.list(tempDir.resolve("backup"))) {
            assertTrue(stream.findAny().isPresent());
        }
    }

    @Test
    public void appliesExplicitMigrationsAfterTheLegacyCutover() throws Exception {
        FileUtil.setDataFolder(tempDir.toFile());
        File configFile = tempDir.resolve("config.yml").toFile();
        YamlConfiguration config = createValidConfig(111);
        config.set("options.extras.obsidianToLava", null);
        config.set("options.island.schematicName", "uSkyBlockDefault");
        config.set("options.general.cooldownRestart", 30);
        config.set("options.general.biomeChange", 60);
        config.set("options.island.islandTeleportDelay", 2);
        config.set("options.island.topTenTimeout", 20);
        config.set("options.party.invite-timeout", 30000);
        config.set("options.advanced.confirmTimeout", 10);
        config.set("options.restart.teleportDelay", 1000);
        config.set("force-replace.options.island.schematicName", "yourschematichere");
        config.set("move-nodes.options.island.fixFlatland", "options.deprecated.fixFlatland");
        config.set("options.deprecated.fixFlatland", true);
        config.save(configFile);

        PluginConfigLoader loader = new PluginConfigLoader(tempDir, new PluginConfigMigrator(Logger.getAnonymousLogger()));
        YamlConfiguration migrated = loader.load();

        assertEquals(loadBundledVersion(), migrated.getInt("version"));
        assertEquals("default", migrated.getString("options.island.schematicName"));
        assertTrue(migrated.getBoolean("options.extras.obsidianToLava"));
        assertEquals("30s", migrated.getString("options.general.cooldownRestart"));
        assertEquals("60s", migrated.getString("options.general.biomeChange"));
        assertEquals("2s", migrated.getString("options.island.islandTeleportDelay"));
        assertEquals("20m", migrated.getString("options.island.topTenTimeout"));
        assertEquals("30s", migrated.getString("options.party.invite-timeout"));
        assertEquals("10s", migrated.getString("options.advanced.confirmTimeout"));
        assertEquals("1000ms", migrated.getString("options.restart.teleportDelay"));
        assertTrue(migrated.getBoolean("options.spawning.guardians.enabled"));
        assertEquals(10, migrated.getInt("options.spawning.guardians.max-per-island"));
        assertEquals(0.1d, migrated.getDouble("options.spawning.guardians.spawn-chance"), 0.00001d);
        assertEquals(RESTART_COOLDOWN_COMMENT, migrated.getComments("options.general.cooldownRestart"));
        assertEquals(INVITE_TIMEOUT_COMMENT, migrated.getComments("options.party.invite-timeout"));
        assertEquals(RESTART_TELEPORT_COMMENT, migrated.getComments("options.restart.teleportDelay"));
        assertEquals(GUARDIAN_ENABLED_COMMENT, migrated.getComments("options.spawning.guardians.enabled"));
        assertEquals(GUARDIAN_CAP_COMMENT, migrated.getComments("options.spawning.guardians.max-per-island"));
        assertEquals(GUARDIAN_CHANCE_COMMENT, migrated.getComments("options.spawning.guardians.spawn-chance"));
        assertFalse(migrated.contains("force-replace"));
        assertFalse(migrated.contains("move-nodes"));
        assertFalse(migrated.contains("options.deprecated.fixFlatland"));
        try (var stream = Files.list(tempDir.resolve("backup"))) {
            assertTrue(stream.findAny().isPresent());
        }
    }

    @Test
    public void normalizesLegacySchematicPlaceholderDuringV112Migration() throws Exception {
        FileUtil.setDataFolder(tempDir.toFile());
        File configFile = tempDir.resolve("config.yml").toFile();
        YamlConfiguration config = createValidConfig(111);
        config.set("options.island.schematicName", "yourschematichere");
        config.set("options.general.cooldownRestart", 30);
        config.set("options.general.biomeChange", 60);
        config.set("options.island.islandTeleportDelay", 2);
        config.set("options.island.topTenTimeout", 20);
        config.set("options.party.invite-timeout", 30000);
        config.set("options.advanced.confirmTimeout", 10);
        config.set("options.restart.teleportDelay", 1000);
        config.save(configFile);

        PluginConfigLoader loader = new PluginConfigLoader(tempDir, new PluginConfigMigrator(Logger.getAnonymousLogger()));
        YamlConfiguration migrated = loader.load();

        assertEquals(loadBundledVersion(), migrated.getInt("version"));
        assertEquals("default", migrated.getString("options.island.schematicName"));
    }

    @Test
    public void migratesSmallNumericInviteTimeoutsAsSeconds() throws Exception {
        FileUtil.setDataFolder(tempDir.toFile());
        File configFile = tempDir.resolve("config.yml").toFile();
        YamlConfiguration config = createValidConfig(112);
        config.set("options.party.invite-timeout", 60);
        config.save(configFile);

        PluginConfigLoader loader = new PluginConfigLoader(tempDir, new PluginConfigMigrator(Logger.getAnonymousLogger()));
        YamlConfiguration migrated = loader.load();

        assertEquals(loadBundledVersion(), migrated.getInt("version"));
        assertEquals("60s", migrated.getString("options.party.invite-timeout"));
        assertEquals(INVITE_TIMEOUT_COMMENT, migrated.getComments("options.party.invite-timeout"));
    }

    @Test
    public void keepsMillisecondInviteTimeoutsWhenTheyAreNotWholeSeconds() throws Exception {
        FileUtil.setDataFolder(tempDir.toFile());
        File configFile = tempDir.resolve("config.yml").toFile();
        YamlConfiguration config = createValidConfig(112);
        config.set("options.party.invite-timeout", 1500);
        config.save(configFile);

        PluginConfigLoader loader = new PluginConfigLoader(tempDir, new PluginConfigMigrator(Logger.getAnonymousLogger()));
        YamlConfiguration migrated = loader.load();

        assertEquals(loadBundledVersion(), migrated.getInt("version"));
        assertEquals("1500ms", migrated.getString("options.party.invite-timeout"));
        assertEquals(INVITE_TIMEOUT_COMMENT, migrated.getComments("options.party.invite-timeout"));
    }

    @Test
    public void failedExplicitMigrationLeavesTheOriginalConfigInPlace() throws Exception {
        FileUtil.setDataFolder(tempDir.toFile());
        File configFile = tempDir.resolve("config.yml").toFile();
        YamlConfiguration config = createValidConfig(112);
        config.set("options.party.invite-timeout", "not-a-duration");
        config.save(configFile);

        PluginConfigLoader loader = new PluginConfigLoader(tempDir, new PluginConfigMigrator(Logger.getAnonymousLogger()));
        try {
            loader.load();
            fail("Expected explicit migration to fail for an invalid invite-timeout");
        } catch (IllegalStateException expected) {
            assertTrue(configFile.isFile());
            YamlConfiguration original = YamlConfiguration.loadConfiguration(configFile);
            assertEquals(112, original.getInt("version"));
            assertEquals("not-a-duration", original.getString("options.party.invite-timeout"));
            try (var stream = Files.list(tempDir.resolve("backup"))) {
                assertTrue(stream.findAny().isPresent());
            }
        }
    }

    @Test
    public void removesLegacyMigrationMetadataFromVersion113Configs() throws Exception {
        FileUtil.setDataFolder(tempDir.toFile());
        File configFile = tempDir.resolve("config.yml").toFile();
        YamlConfiguration config = createValidConfig(113);
        config.set("force-replace.options.island.schematicName", "yourschematichere");
        config.set("move-nodes.options.island.fixFlatland", "options.deprecated.fixFlatland");
        config.set("options.deprecated.fixFlatland", true);
        config.save(configFile);

        PluginConfigLoader loader = new PluginConfigLoader(tempDir, new PluginConfigMigrator(Logger.getAnonymousLogger()));
        YamlConfiguration migrated = loader.load();

        assertEquals(loadBundledVersion(), migrated.getInt("version"));
        assertFalse(migrated.contains("force-replace"));
        assertFalse(migrated.contains("move-nodes"));
        assertFalse(migrated.contains("options.deprecated.fixFlatland"));
        assertFalse(migrated.contains("options.deprecated"));
    }

    @Test
    public void addsGuardianHabitatDefaultsDuringMigration115() throws Exception {
        FileUtil.setDataFolder(tempDir.toFile());
        File configFile = tempDir.resolve("config.yml").toFile();
        YamlConfiguration config = createValidConfig(114);
        config.set("options.spawning.guardians", null);
        config.save(configFile);

        PluginConfigLoader loader = new PluginConfigLoader(tempDir, new PluginConfigMigrator(Logger.getAnonymousLogger()));
        YamlConfiguration migrated = loader.load();

        assertEquals(loadBundledVersion(), migrated.getInt("version"));
        assertTrue(migrated.getBoolean("options.spawning.guardians.enabled"));
        assertEquals(10, migrated.getInt("options.spawning.guardians.max-per-island"));
        assertEquals(0.1d, migrated.getDouble("options.spawning.guardians.spawn-chance"), 0.00001d);
        assertEquals(GUARDIAN_ENABLED_COMMENT, migrated.getComments("options.spawning.guardians.enabled"));
        assertEquals(GUARDIAN_CAP_COMMENT, migrated.getComments("options.spawning.guardians.max-per-island"));
        assertEquals(GUARDIAN_CHANCE_COMMENT, migrated.getComments("options.spawning.guardians.spawn-chance"));
    }

    @Test
    public void addsExplicitSchemePathsDuringMigration116() throws Exception {
        FileUtil.setDataFolder(tempDir.toFile());
        Files.createDirectories(tempDir.resolve("schematics"));
        Files.writeString(tempDir.resolve("schematics/default.schematic"), "default");
        Files.writeString(tempDir.resolve("schematics/skySMP.schematic"), "sky");
        Files.writeString(tempDir.resolve("schematics/uSkyBlockNether.schem"), "nether");

        File configFile = tempDir.resolve("config.yml").toFile();
        YamlConfiguration config = createValidConfig(115);
        config.set("nether.enabled", true);
        config.set("nether.schematicName", "uSkyBlockNether");
        config.set("island-schemes.default.enabled", true);
        config.set("island-schemes.skySMP.enabled", false);
        config.set("island-schemes.spawn.enabled", false);
        config.save(configFile);

        PluginConfigLoader loader = new PluginConfigLoader(tempDir, new PluginConfigMigrator(Logger.getAnonymousLogger()));
        YamlConfiguration migrated = loader.load();

        assertEquals(loadBundledVersion(), migrated.getInt("version"));
        assertEquals("default.schematic", migrated.getString("island-schemes.default.schematic"));
        assertEquals("uSkyBlockNether.schem", migrated.getString("island-schemes.default.nether-schematic"));
        assertEquals("skySMP.schematic", migrated.getString("island-schemes.skySMP.schematic"));
        assertEquals("uSkyBlockNether.schem", migrated.getString("island-schemes.skySMP.nether-schematic"));
        assertFalse(migrated.contains("island-schemes.spawn"));
        assertFalse(migrated.contains("nether.schematicName"));
    }

    @Test
    public void movesLegacyMiskeyedRuntimePathsDuringMigration117() throws Exception {
        FileUtil.setDataFolder(tempDir.toFile());
        File configFile = tempDir.resolve("config.yml").toFile();
        YamlConfiguration config = createValidConfig(116);
        config.set("options.general.maxSpam", null);
        config.set("general.maxSpam", 1700);
        config.set("options.protection.visitors.vehicle-damage", null);
        config.set("options.protection.visitors.vehicle-break", true);
        config.set("options.protection.nether-roof", true);
        config.set("asyncworldedit.progressEveryMs", 5000);
        config.set("asyncworldedit.progressEveryPct", 20);
        config.set("nether.activate-at.level", 100);
        config.save(configFile);

        PluginConfigLoader loader = new PluginConfigLoader(tempDir, new PluginConfigMigrator(Logger.getAnonymousLogger()));
        YamlConfiguration migrated = loader.load();

        assertEquals(loadBundledVersion(), migrated.getInt("version"));
        assertEquals(1700, migrated.getInt("options.general.maxSpam"));
        assertTrue(migrated.getBoolean("options.protection.visitors.vehicle-damage"));
        assertFalse(migrated.contains("general.maxSpam"));
        assertFalse(migrated.contains("options.protection.visitors.vehicle-break"));
        assertFalse(migrated.contains("options.protection.nether-roof"));
        assertFalse(migrated.contains("asyncworldedit.progressEveryMs"));
        assertFalse(migrated.contains("asyncworldedit.progressEveryPct"));
        assertFalse(migrated.contains("nether.activate-at"));
    }

    @Test
    public void rejectsFutureConfigVersions() throws Exception {
        FileUtil.setDataFolder(tempDir.toFile());
        File configFile = tempDir.resolve("config.yml").toFile();
        YamlConfiguration config = new YamlConfiguration();
        config.set("version", 999);
        config.save(configFile);

        PluginConfigLoader loader = new PluginConfigLoader(tempDir, new PluginConfigMigrator(Logger.getAnonymousLogger()));
        try {
            loader.load();
            fail("Expected future config versions to be rejected");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("newer than the supported version"));
        }
    }

    @Test
    public void loadsBundledCurrentConfig() throws Exception {
        FileUtil.setDataFolder(tempDir.toFile());
        File configFile = tempDir.resolve("config.yml").toFile();
        YamlConfiguration config = loadBundledConfig();
        config.save(configFile);

        PluginConfigLoader loader = new PluginConfigLoader(tempDir, new PluginConfigMigrator(Logger.getAnonymousLogger()));
        YamlConfiguration loaded = loader.load();
        YamlConfiguration bundled = loadBundledConfig();

        assertEquals(bundled.saveToString(), loaded.saveToString());
    }

    @Test
    public void rejectsLocalizedConfigFileNamesWithoutCanonicalConfig() throws Exception {
        FileUtil.setDataFolder(tempDir.toFile());
        File localizedConfigFile = tempDir.resolve("config_" + Locale.getDefault() + ".yml").toFile();
        YamlConfiguration config = loadBundledConfig();
        config.save(localizedConfigFile);

        PluginConfigLoader loader = new PluginConfigLoader(tempDir, new PluginConfigMigrator(Logger.getAnonymousLogger()));
        try {
            loader.load();
            fail("Expected localized config filenames to be rejected");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains(localizedConfigFile.getName()));
            assertTrue(expected.getMessage().contains("config.yml"));
        }
    }

    @Test
    public void currentVersionConfigsAreNotRejectedByTheLoaderForSchemaShape() throws Exception {
        FileUtil.setDataFolder(tempDir.toFile());
        File configFile = tempDir.resolve("config.yml").toFile();
        YamlConfiguration config = createValidConfig(loadBundledVersion());
        config.set("options.advanced.manageSpawn", null);
        config.save(configFile);

        PluginConfigLoader loader = new PluginConfigLoader(tempDir, new PluginConfigMigrator(Logger.getAnonymousLogger()));
        YamlConfiguration loaded = loader.load();

        assertEquals(loadBundledVersion(), loaded.getInt("version"));
        assertFalse(loaded.isSet("options.advanced.manageSpawn"));
    }

    private YamlConfiguration createValidConfig(int version) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("version", version);
        config.set("language", "en");
        config.set("options.general.maxPartySize", 4);
        config.set("options.general.worldName", "skyworld");
        config.set("options.general.cooldownRestart", "30s");
        config.set("options.general.biomeChange", "60s");
        config.set("options.general.spawnSize", 64);
        config.set("options.island.height", 150);
        config.set("options.island.islandTeleportDelay", "2s");
        config.set("options.island.topTenTimeout", "20m");
        config.set("options.island.schematicName", "default");
        config.set("options.extras.obsidianToLava", true);
        config.set("options.party.invite-timeout", "2m");
        config.set("options.advanced.confirmTimeout", "10s");
        config.set("options.advanced.manageSpawn", true);
        config.set("options.restart.teleportDelay", "1000ms");
        if (version >= 115) {
            config.set("options.spawning.guardians.enabled", true);
            config.set("options.spawning.guardians.max-per-island", 10);
            config.set("options.spawning.guardians.spawn-chance", 0.1d);
        }
        config.set("island-schemes.default.enabled", true);
        if (version >= 116) {
            config.set("island-schemes.default.schematic", "default.schematic");
            config.set("island-schemes.default.nether-schematic", "uSkyBlockNether.schem");
        } else {
            config.set("nether.schematicName", "uSkyBlockNether");
        }
        config.set("nether.enabled", false);
        return config;
    }

    private int loadBundledVersion() {
        YamlConfiguration bundled = loadBundledConfig();
        return bundled.getInt("version");
    }

    private YamlConfiguration loadBundledConfig() {
        YamlConfiguration bundled = new YamlConfiguration();
        try (var reader = new java.io.InputStreamReader(
            Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(PluginConfigLoader.CONFIG_NAME)),
            java.nio.charset.StandardCharsets.UTF_8)) {
            bundled.load(reader);
            return bundled;
        } catch (Exception e) {
            throw new AssertionError("Unable to load bundled config.yml", e);
        }
    }
}
