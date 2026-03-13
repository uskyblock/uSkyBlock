package us.talabrek.ultimateskyblock.handler;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import us.talabrek.ultimateskyblock.config.PluginConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
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

        SchematicHandler handler = new SchematicHandler(Logger.getAnonymousLogger(), pluginConfig(config), tempDir);
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

        SchematicHandler handler = new SchematicHandler(Logger.getAnonymousLogger(), pluginConfig(config), tempDir);
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

        SchematicHandler handler = new SchematicHandler(Logger.getAnonymousLogger(), pluginConfig(config), tempDir);

        assertTrue(handler.getSchemeNames().isEmpty());
    }

    private static PluginConfig pluginConfig(YamlConfiguration yamlConfig) {
        PluginConfig pluginConfig = mock(PluginConfig.class);
        when(pluginConfig.getYamlConfig()).thenReturn(yamlConfig);
        return pluginConfig;
    }
}
