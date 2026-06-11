package us.talabrek.ultimateskyblock.world;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigFactory;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.hook.HookManager;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WorldManagerTest {

    private Server server;
    private WorldManager worldManager;
    private TestLogHandler logHandler;

    @BeforeEach
    public void setUp() throws Exception {
        server = BukkitServerMock.setupServerMock();
        WorldManager.skyBlockWorld = null;
        WorldManager.skyBlockNetherWorld = null;

        logHandler = new TestLogHandler();
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(logHandler);

        YamlConfiguration config = new YamlConfiguration();
        config.setDefaults(PluginConfigLoader.loadBundledConfig());
        // Separate logger for the factory: config loading may log on its own, which would
        // pollute the assertions on logHandler.
        RuntimeConfig runtimeConfig =
            new RuntimeConfigFactory(new GameObjectFactory(), Logger.getAnonymousLogger()).load(config);
        RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
        when(runtimeConfigs.current()).thenReturn(runtimeConfig);

        uSkyBlock plugin = mock(uSkyBlock.class);
        when(plugin.getDataFolder()).thenReturn(new File("build/tmp/world-manager-test"));

        worldManager = new WorldManager(
            plugin, logger, runtimeConfigs, mock(HookManager.class), mock(Scheduler.class));
    }

    @AfterEach
    public void tearDown() {
        WorldManager.skyBlockWorld = null;
        WorldManager.skyBlockNetherWorld = null;
    }

    @Test
    public void getWorldWarnsWhenLoadedWorldKeepsWrongGenerator() {
        // Simulates the level-name / generator-less Multiverse case: the world is already
        // loaded with the vanilla generator, and Bukkit's createWorld() returns it unchanged.
        World loaded = mock(World.class);
        when(loaded.getName()).thenReturn("skyworld");
        when(loaded.getGenerator()).thenReturn(null);
        when(server.getWorld("skyworld")).thenReturn(loaded);
        when(server.createWorld(any(WorldCreator.class))).thenReturn(loaded);

        World result = worldManager.getWorld();

        assertThat(result, is(loaded));
        assertThat(logHandler.severeMessages(), containsString("generator: uSkyBlock"));
        assertThat(logHandler.severeMessages(), containsString("skyworld"));
    }

    @Test
    public void getWorldStaysQuietWhenGeneratorIsAttached() {
        World created = mock(World.class);
        when(created.getName()).thenReturn("skyworld");
        when(created.getGenerator()).thenReturn(new SkyBlockChunkGenerator());
        when(server.getWorld("skyworld")).thenReturn(null);
        when(server.createWorld(any(WorldCreator.class))).thenReturn(created);

        worldManager.getWorld();

        assertThat(logHandler.severeMessages(), is(""));
    }

    @Test
    public void hasExpectedGeneratorIsFalseWhenNoGeneratorAttached() {
        World world = mock(World.class);
        when(world.getGenerator()).thenReturn(null);

        assertFalse(WorldManager.hasExpectedGenerator(world, new SkyBlockChunkGenerator()));
    }

    @Test
    public void hasExpectedGeneratorIsTrueForSameGeneratorClass() {
        World world = mock(World.class);
        when(world.getGenerator()).thenReturn(new SkyBlockChunkGenerator());

        assertTrue(WorldManager.hasExpectedGenerator(world, new SkyBlockChunkGenerator()));
    }

    @Test
    public void hasExpectedGeneratorIsFalseForDifferentGeneratorClass() {
        World world = mock(World.class);
        when(world.getGenerator()).thenReturn(new SkyBlockNetherChunkGenerator());

        assertFalse(WorldManager.hasExpectedGenerator(world, new SkyBlockChunkGenerator()));
    }

    @Test
    public void wrongGeneratorWarningContainsActionableInstructions() {
        List<String> lines = WorldManager.wrongGeneratorWarning("skyworld");

        String joined = String.join("\n", lines);
        assertThat(joined, containsString("skyworld"));
        assertThat(joined, containsString("worlds:"));
        assertThat(joined, containsString("generator: uSkyBlock"));
        assertThat(joined, containsString("/usb chunk regen"));
    }

    private static final class TestLogHandler extends Handler {
        private final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        String severeMessages() {
            return records.stream()
                .filter(record -> record.getLevel() == Level.SEVERE)
                .map(LogRecord::getMessage)
                .collect(Collectors.joining("\n"));
        }
    }
}
