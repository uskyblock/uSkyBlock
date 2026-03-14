package us.talabrek.ultimateskyblock.island;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.stubbing.Answer;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigFactory;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.world.WorldManager;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IslandLocatorLogicTest {
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        BukkitServerMock.setupServerMock();
    }

    @Test
    public void testNextIslandLocation() throws Exception {
        Location p = new Location(null, 0, 0, 0);
        File csvFile = File.createTempFile("newislands", ".csv");
        PrintWriter writer = new PrintWriter(new FileWriter(csvFile));
        for (int i = 0; i < 49; i++) {
            p = IslandLocatorLogic.nextIslandLocation(p, 1, 120);
            writer.println(p.getBlockX() + ";" + p.getBlockZ());
        }
        System.out.println("Wrote first 49 island locations to " + csvFile);
    }

    @Test
    public void testNextIslandLocationReservation() throws Exception {
        uSkyBlock plugin = createPluginMock();
        IslandLocatorLogic locator = new IslandLocatorLogic(plugin, Files.createDirectory(tempDir.resolve("reservation-1")), mock(), mock(), mock(), mock(), createRuntimeConfigs(10));
        Player player = createPlayerMock();
        Location location1 = locator.getNextIslandLocation(player);
        assertThat(location1, notNullValue());
        Location location2 = locator.getNextIslandLocation(player);
        assertThat(location2, notNullValue());
        assertThat(location1, is(not(location2)));
    }

    @Test
    public void testNextIslandLocationReservationConcurrency() throws Exception {
        uSkyBlock plugin = createPluginMock();
        final IslandLocatorLogic locator = new IslandLocatorLogic(plugin, Files.createDirectory(tempDir.resolve("reservation-2")), mock(), mock(), mock(), mock(), createRuntimeConfigs(10));
        final List<Location> locations = new ArrayList<>();
        ThreadGroup threadGroup = new ThreadGroup("My");
        for (int i = 0; i < 10; i++) {
            Thread t = new Thread(threadGroup, () -> {
                Player player = createPlayerMock();
                Location location = locator.getNextIslandLocation(player);
                locations.add(location);
            });
            t.start();
        }
        while (threadGroup.activeCount() > 0) {
            Thread.sleep(10);
        }
        Set<Location> set = new HashSet<>(locations);
        assertThat(locations.size(), greaterThan(0));
        assertThat("duplicate locations detected", set.size(), is(locations.size()));
    }

    private Player createPlayerMock() {
        Player player = mock(Player.class);
        when(player.getLocation()).then((Answer<Location>) invocationOnMock -> new Location(null, 100, 100, 100));
        return player;
    }

    private uSkyBlock createPluginMock() {
        uSkyBlock plugin = mock(uSkyBlock.class);
        FileConfiguration config = new YamlConfiguration();
        when(plugin.getConfig()).thenReturn(config);
        OrphanLogic orphanLogic = mock(OrphanLogic.class);
        when(plugin.getOrphanLogic()).thenReturn(orphanLogic);
        WorldManager worldManager = mock(WorldManager.class);
        when(worldManager.isSkyWorld(any(World.class))).thenReturn(true);
        when(plugin.getWorldManager()).thenReturn(worldManager);
        return plugin;
    }

    private RuntimeConfigs createRuntimeConfigs(int islandDistance) {
        YamlConfiguration config = new YamlConfiguration();
        config.setDefaults(PluginConfigLoader.loadBundledConfig());
        config.set("language", "en");
        config.set("options.general.worldName", "skyworld");
        config.set("options.general.cooldownRestart", "1m");
        config.set("options.general.biomeChange", "1m");
        config.set("options.general.defaultBiome", "plains");
        config.set("options.general.defaultNetherBiome", "nether_wastes");
        config.set("options.advanced.confirmTimeout", "30s");
        config.set("options.advanced.playerDB.storage", "file");
        config.set("options.party.invite-timeout", "1m");
        config.set("options.island.distance", islandDistance);
        config.set("options.island.height", 120);
        config.set("options.island.topTenTimeout", "15m");
        config.set("options.island.islandTeleportDelay", "0s");
        config.set("options.island.chatFormat", "default");
        config.set("options.party.chatFormat", "default");
        config.set("nether.chunkgenerator", "default");
        config.set("options.advanced.islandSaveEvery", "1m");
        config.set("options.advanced.playerSaveEvery", "1m");
        config.set("options.async.maxIterationTime", "50ms");
        config.set("options.async.maxConsecutiveTicks", 1L);
        config.set("options.async.yieldDelay", "1t");
        config.set("options.advanced.playerDB.nameCacheSpec", "maximumSize=100");
        config.set("options.advanced.playerDB.uuidCacheSpec", "maximumSize=100");
        config.set("options.advanced.playerCacheSpec", "maximumSize=100");
        config.set("options.advanced.islandCacheSpec", "maximumSize=100");
        config.set("plugin-updates.branch", "LATEST");
        RuntimeConfig runtimeConfig = new RuntimeConfigFactory(new GameObjectFactory(), Logger.getAnonymousLogger()).load(config);

        RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
        when(runtimeConfigs.current()).thenReturn(runtimeConfig);
        return runtimeConfigs;
    }
}
