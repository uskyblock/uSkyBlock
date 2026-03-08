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

        assertEquals(PluginConfigLoader.LEGACY_BASELINE_VERSION, config.getInt("version"));
        assertTrue(config.contains("options.general.worldName"));
        assertTrue(config.contains("options.general.spawnSize"));
        assertTrue(config.contains("options.island.schematicName"));
        assertTrue(config.contains("options.advanced.manageSpawn"));
        assertTrue(new File(testFolder.getRoot(), "config.yml.old").isFile());
        try (var stream = Files.list(new File(testFolder.getRoot(), "backup").toPath())) {
            assertTrue(stream.findAny().isPresent());
        }
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
    public void rejectsMalformedCurrentVersionConfig() throws Exception {
        FileUtil.setDataFolder(testFolder.getRoot());
        File configFile = new File(testFolder.getRoot(), "config.yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("version", PluginConfigLoader.LEGACY_BASELINE_VERSION);
        config.set("language", "en");
        config.set("options.general.maxPartySize", 4);
        config.set("options.general.worldName", "skyworld");
        config.set("options.general.spawnSize", 64);
        config.set("options.island.height", 150);
        config.set("options.island.schematicName", "default");
        config.set("options.restart.teleportDelay", 1000);
        config.set("nether.enabled", false);
        config.save(configFile);

        PluginConfigLoader loader = new PluginConfigLoader(Logger.getAnonymousLogger());
        try {
            loader.load();
            fail("Expected malformed current-version configs to be rejected");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("Missing required path"));
        }
    }
}
