package us.talabrek.ultimateskyblock.handler;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigFactory;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SchematicHandlerTest {
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        BukkitServerMock.setupServerMock();
    }

    @Test
    public void resolveTargetAcceptsNormalEntry() throws Exception {
        Path root = tempDir.resolve("plugins/uSkyBlock/schematics");
        Optional<Path> result = SchematicHandler.resolveTarget(root, "default.schematic");

        assertThat("Expected normal schematic path to resolve", result.isPresent(), is(true));
        assertThat(result.get().startsWith(root.toAbsolutePath().normalize()), is(true));
        assertThat(result.get().getFileName().toString(), is("default.schematic"));
    }

    @Test
    public void resolveTargetRejectsTraversal() throws Exception {
        Path root = tempDir.resolve("plugins/uSkyBlock/schematics");
        Optional<Path> result = SchematicHandler.resolveTarget(root, "../evil.schematic");

        assertThat("Traversal paths must be rejected", result.isPresent(), is(false));
    }

    @Test
    public void getSchemeUsesExplicitConfiguredPaths() throws Exception {
        Files.createDirectories(tempDir.resolve("schematics"));
        Files.writeString(tempDir.resolve("schematics/default.schematic"), "default");
        Files.writeString(tempDir.resolve("schematics/uSkyBlockNether.schem"), "nether");

        YamlConfiguration config = new YamlConfiguration();
        config.set("nether.enabled", true);
        config.set("island-schemes.default.enabled", true);
        config.set("island-schemes.default.schematic", "default.schematic");
        config.set("island-schemes.default.nether-schematic", "uSkyBlockNether.schem");

        SchematicHandler handler = new SchematicHandler(Logger.getAnonymousLogger(), runtimeConfigs(config), tempDir);
        SchematicHandler.SchematicPair pair = handler.getScheme("default");

        assertNotNull(pair);
        assertEquals(tempDir.resolve("schematics/default.schematic"), pair.overworld());
        assertTrue(pair.nether().isPresent());
        assertEquals(tempDir.resolve("schematics/uSkyBlockNether.schem"), pair.nether().get());
    }

    @Test
    public void getSchemeIgnoresNetherSchematicWhenNetherIsDisabled() throws Exception {
        Files.createDirectories(tempDir.resolve("schematics"));
        Files.writeString(tempDir.resolve("schematics/default.schematic"), "default");

        YamlConfiguration config = new YamlConfiguration();
        config.set("nether.enabled", false);
        config.set("island-schemes.default.enabled", true);
        config.set("island-schemes.default.schematic", "default.schematic");
        config.set("island-schemes.default.nether-schematic", "missing.schem");

        SchematicHandler handler = new SchematicHandler(Logger.getAnonymousLogger(), runtimeConfigs(config), tempDir);
        SchematicHandler.SchematicPair pair = handler.getScheme("default");

        assertNotNull(pair);
        assertEquals(tempDir.resolve("schematics/default.schematic"), pair.overworld());
        assertTrue(pair.nether().isEmpty());
    }

    @Test
    public void getSchemeNamesRequireNetherSchematicWhenNetherIsEnabled() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("nether.enabled", true);
        config.set("island-schemes.default.enabled", true);
        config.set("island-schemes.default.schematic", "default.schematic");
        config.set("island-schemes.default.nether-schematic", "");

        SchematicHandler handler = new SchematicHandler(Logger.getAnonymousLogger(), runtimeConfigs(config), tempDir);

        assertTrue(handler.getSchemeNames().isEmpty());
    }

    @Test
    public void initializeWarnsWhenDefaultSchemeDoesNotExist() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("options.island.schematicName", "missing");
        config.set("island-schemes.default.enabled", true);
        config.set("island-schemes.default.schematic", "default.schematic");

        TestLogHandler logHandler = new TestLogHandler();
        Logger logger = testLogger(logHandler);
        SchematicHandler handler = new SchematicHandler(logger, runtimeConfigs(config), tempDir);

        handler.initialize(mock(Plugin.class));

        assertTrue(logHandler.messages().stream()
            .anyMatch(message -> message.contains("Configured default island scheme 'missing' does not exist.")));
    }

    @Test
    public void initializeWarnsWhenDefaultSchemeIsDisabled() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("options.island.schematicName", "default");
        config.set("island-schemes.default.enabled", false);
        config.set("island-schemes.default.schematic", "default.schematic");

        TestLogHandler logHandler = new TestLogHandler();
        Logger logger = testLogger(logHandler);
        SchematicHandler handler = new SchematicHandler(logger, runtimeConfigs(config), tempDir);

        handler.initialize(mock(Plugin.class));

        assertTrue(logHandler.messages().stream()
            .anyMatch(message -> message.contains("Configured default island scheme 'default' is disabled.")));
    }

    private static RuntimeConfigs runtimeConfigs(YamlConfiguration yamlConfig) {
        yamlConfig.setDefaults(PluginConfigLoader.loadBundledConfig());
        var runtimeConfig = new RuntimeConfigFactory(new GameObjectFactory()).load(yamlConfig);
        RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
        when(runtimeConfigs.current()).thenReturn(runtimeConfig);
        return runtimeConfigs;
    }

    private static Logger testLogger(TestLogHandler handler) {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        logger.addHandler(handler);
        return logger;
    }

    private static final class TestLogHandler extends Handler {
        private final List<String> messages = new ArrayList<>();

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

        private List<String> messages() {
            return messages;
        }
    }
}
