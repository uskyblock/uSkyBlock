package us.talabrek.ultimateskyblock;

import dk.lockfuglsang.minecraft.file.FileUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PluginConfigLoaderTest {
    private static final List<String> RESTART_COOLDOWN_COMMENT = List.of(
        "# [duration] The time before a player can use the /island restart command again. Use ms, s, m, h, or d.");
    private static final List<String> INVITE_TIMEOUT_COMMENT = List.of(
        "# [duration] How long an island invite stays valid. Use ms, s, m, h, or d.");
    private static final List<String> RESTART_TELEPORT_COMMENT = List.of(
        "# [duration] The time to wait before porting the player back on /is restart or /is create. Use ms, s, m, h, or d.");

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void upgradesLegacyConfigThroughTheCurrentCompatibilityPath() throws Exception {
        FileUtil.setDataFolder(testFolder.getRoot());
        File configFile = new File(testFolder.getRoot(), "config.yml");
        try (var reader = Objects.requireNonNull(getClass().getResourceAsStream("imports/old-config.yml"))) {
            Files.copy(reader, configFile.toPath());
        }

        PluginConfigLoader loader = new PluginConfigLoader(Logger.getAnonymousLogger());
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
        assertTrue(new File(testFolder.getRoot(), "config.yml.old").isFile());
        try (var stream = Files.list(new File(testFolder.getRoot(), "backup").toPath())) {
            assertTrue(stream.findAny().isPresent());
        }
    }

    @Test
    public void appliesExplicitMigrationsAfterTheLegacyCutover() throws Exception {
        FileUtil.setDataFolder(testFolder.getRoot());
        File configFile = new File(testFolder.getRoot(), "config.yml");
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
        config.save(configFile);

        PluginConfigLoader loader = new PluginConfigLoader(Logger.getAnonymousLogger());
        YamlConfiguration migrated = loader.load();

        assertEquals(113, migrated.getInt("version"));
        assertEquals("default", migrated.getString("options.island.schematicName"));
        assertTrue(migrated.getBoolean("options.extras.obsidianToLava"));
        assertEquals("30s", migrated.getString("options.general.cooldownRestart"));
        assertEquals("60s", migrated.getString("options.general.biomeChange"));
        assertEquals("2s", migrated.getString("options.island.islandTeleportDelay"));
        assertEquals("20m", migrated.getString("options.island.topTenTimeout"));
        assertEquals("30s", migrated.getString("options.party.invite-timeout"));
        assertEquals("10s", migrated.getString("options.advanced.confirmTimeout"));
        assertEquals("1000ms", migrated.getString("options.restart.teleportDelay"));
        assertEquals(RESTART_COOLDOWN_COMMENT, migrated.getComments("options.general.cooldownRestart"));
        assertEquals(INVITE_TIMEOUT_COMMENT, migrated.getComments("options.party.invite-timeout"));
        assertEquals(RESTART_TELEPORT_COMMENT, migrated.getComments("options.restart.teleportDelay"));
    }

    @Test
    public void migratesSmallNumericInviteTimeoutsAsSeconds() throws Exception {
        FileUtil.setDataFolder(testFolder.getRoot());
        File configFile = new File(testFolder.getRoot(), "config.yml");
        YamlConfiguration config = createValidConfig(112);
        config.set("options.party.invite-timeout", 60);
        config.save(configFile);

        PluginConfigLoader loader = new PluginConfigLoader(Logger.getAnonymousLogger());
        YamlConfiguration migrated = loader.load();

        assertEquals(113, migrated.getInt("version"));
        assertEquals("60s", migrated.getString("options.party.invite-timeout"));
        assertEquals(INVITE_TIMEOUT_COMMENT, migrated.getComments("options.party.invite-timeout"));
    }

    @Test
    public void keepsMillisecondInviteTimeoutsWhenTheyAreNotWholeSeconds() throws Exception {
        FileUtil.setDataFolder(testFolder.getRoot());
        File configFile = new File(testFolder.getRoot(), "config.yml");
        YamlConfiguration config = createValidConfig(112);
        config.set("options.party.invite-timeout", 1500);
        config.save(configFile);

        PluginConfigLoader loader = new PluginConfigLoader(Logger.getAnonymousLogger());
        YamlConfiguration migrated = loader.load();

        assertEquals(113, migrated.getInt("version"));
        assertEquals("1500ms", migrated.getString("options.party.invite-timeout"));
        assertEquals(INVITE_TIMEOUT_COMMENT, migrated.getComments("options.party.invite-timeout"));
    }

    @Test
    public void rejectsFutureConfigVersions() throws Exception {
        FileUtil.setDataFolder(testFolder.getRoot());
        File configFile = new File(testFolder.getRoot(), "config.yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("version", 999);
        config.save(configFile);

        PluginConfigLoader loader = new PluginConfigLoader(Logger.getAnonymousLogger());
        try {
            loader.load();
            fail("Expected future config versions to be rejected");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("newer than the supported version"));
        }
    }

    @Test
    public void loadsBundledCurrentConfig() throws Exception {
        FileUtil.setDataFolder(testFolder.getRoot());
        File configFile = new File(testFolder.getRoot(), "config.yml");
        YamlConfiguration config = loadBundledConfig();
        config.save(configFile);

        PluginConfigLoader loader = new PluginConfigLoader(Logger.getAnonymousLogger());
        YamlConfiguration loaded = loader.load();
        YamlConfiguration bundled = loadBundledConfig();

        assertEquals(bundled.saveToString(), loaded.saveToString());
    }

    @Test
    public void currentVersionConfigsAreNotRejectedByTheLoaderForSchemaShape() throws Exception {
        FileUtil.setDataFolder(testFolder.getRoot());
        File configFile = new File(testFolder.getRoot(), "config.yml");
        YamlConfiguration config = createValidConfig(loadBundledVersion());
        config.set("options.advanced.manageSpawn", null);
        config.save(configFile);

        PluginConfigLoader loader = new PluginConfigLoader(Logger.getAnonymousLogger());
        YamlConfiguration loaded = loader.load();

        assertEquals(loadBundledVersion(), loaded.getInt("version"));
        assertFalse(loaded.contains("options.advanced.manageSpawn"));
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
