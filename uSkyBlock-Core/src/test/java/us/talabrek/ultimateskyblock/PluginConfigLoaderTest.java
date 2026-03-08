package us.talabrek.ultimateskyblock;

import dk.lockfuglsang.minecraft.file.FileUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.util.Objects;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PluginConfigLoaderTest {

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
        YamlConfiguration bundled = loadBundledConfig();

        assertMatchesBundledDefaults(config, bundled);
        assertTrue(new File(testFolder.getRoot(), "config.yml.old").isFile());
        try (var stream = Files.list(new File(testFolder.getRoot(), "backup").toPath())) {
            assertTrue(stream.findAny().isPresent());
        }
    }

    @Test
    public void appliesExplicitMigrationsAfterTheLegacyCutover() throws Exception {
        FileUtil.setDataFolder(testFolder.getRoot());
        File configFile = new File(testFolder.getRoot(), "config.yml");
        YamlConfiguration config = createValidConfig(PluginConfigLoader.LEGACY_BASELINE_VERSION);
        config.set("options.extras.obsidianToLava", null);
        config.set("options.island.schematicName", "uSkyBlockDefault");
        config.save(configFile);

        PluginConfigLoader loader = new PluginConfigLoader(Logger.getAnonymousLogger());
        YamlConfiguration migrated = loader.load();
        YamlConfiguration bundled = loadBundledConfig();

        assertMatchesBundledDefaults(migrated, bundled);
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

        assertMatchesBundledDefaults(loaded, bundled);
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
        config.set("options.general.spawnSize", 64);
        config.set("options.island.height", 150);
        config.set("options.island.schematicName", "default");
        config.set("options.extras.obsidianToLava", true);
        config.set("options.advanced.manageSpawn", true);
        config.set("options.restart.teleportDelay", 1000);
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

    private void assertMatchesBundledDefaults(YamlConfiguration config, YamlConfiguration bundled) {
        assertEquals(bundled.getInt("version"), config.getInt("version"));
        assertEquals(bundled.getString("options.general.worldName"), config.getString("options.general.worldName"));
        assertEquals(bundled.getInt("options.general.spawnSize"), config.getInt("options.general.spawnSize"));
        assertEquals(bundled.getString("options.island.schematicName"), config.getString("options.island.schematicName"));
        assertEquals(bundled.getBoolean("options.extras.obsidianToLava"), config.getBoolean("options.extras.obsidianToLava"));
    }
}
