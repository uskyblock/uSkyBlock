package us.talabrek.ultimateskyblock.island;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.world.WorldManager;

import java.io.File;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Exercises the {@link IslandInfo} warp state machine (setWarpLocation / hasWarp / setWarp /
 * getWarpLocation) against a REAL IslandInfo backed by a temp island config file.
 *
 * <p>These warp methods only read/write the island's own YAML config (plus
 * {@code plugin.getWorldManager().getWorld()} to reconstruct a {@link Location}), so no
 * RuntimeConfig is required and RuntimeConfigs#current() is never stubbed.</p>
 *
 * <p>Not covered here, deferred to the live-server harness: the {@code lock()} guard that
 * deactivates an active warp (IslandInfo#lock, ~line 631). {@code lock()} calls the static
 * {@code WorldGuardHandler.islandLock()}, whose body references WorldGuard
 * {@code RegionManager}/{@code ProtectedRegion} types. WorldGuard is a compileOnly dependency and
 * is absent from the unit-test classpath, so invoking {@code lock()} would fail with
 * NoClassDefFoundError before ever reaching the warp-deactivation guard.</p>
 */
public class IslandInfoWarpTest {
    @TempDir
    Path tempDir;

    private uSkyBlock plugin;
    private RuntimeConfigs runtimeConfigs;

    @BeforeEach
    public void setUp() {
        plugin = mock(uSkyBlock.class);
        runtimeConfigs = mock(RuntimeConfigs.class);
        // getWarpLocation() reconstructs a Location via plugin.getWorldManager().getWorld();
        // a WorldManager whose getWorld() returns null (the mock default) is enough to assert the
        // stored block coordinates and orientation.
        WorldManager worldManager = mock(WorldManager.class);
        doReturn(worldManager).when(plugin).getWorldManager();
    }

    @Test
    public void freshIslandHasNoActiveWarp() throws Exception {
        IslandInfo island = createIslandInfo();

        assertThat(island.hasWarp(), is(false));
        assertThat(island.getWarpLocation(), is(nullValue()));
    }

    @Test
    public void setWarpLocationActivatesWarp() throws Exception {
        IslandInfo island = createIslandInfo();

        island.setWarpLocation(new Location(null, 100, 64, -200, 90.0f, 45.0f));

        assertThat(island.hasWarp(), is(true));
    }

    @Test
    public void nullWarpLocationIsIgnored() throws Exception {
        IslandInfo island = createIslandInfo();

        island.setWarpLocation(null);

        assertThat(island.hasWarp(), is(false));
        assertThat(island.getWarpLocation(), is(nullValue()));
    }

    @Test
    public void getWarpLocationReconstructsBlockCoordsAndOrientation() throws Exception {
        IslandInfo island = createIslandInfo();
        island.setWarpLocation(new Location(null, 100, 64, -200, 90.0f, 45.0f));

        Location warp = island.getWarpLocation();

        assertThat(warp, is(notNullValue()));
        assertThat(warp.getBlockX(), is(100));
        assertThat(warp.getBlockY(), is(64));
        assertThat(warp.getBlockZ(), is(-200));
        assertThat(warp.getYaw(), is(90.0f));
        assertThat(warp.getPitch(), is(45.0f));
    }

    @Test
    public void setWarpTogglesActiveFlagAndGatesGetWarpLocation() throws Exception {
        IslandInfo island = createIslandInfo();
        island.setWarpLocation(new Location(null, 10, 70, 20, 0.0f, 0.0f));
        assertThat(island.hasWarp(), is(true));

        island.setWarp(false);
        assertThat(island.hasWarp(), is(false));
        assertThat(island.getWarpLocation(), is(nullValue()));

        island.setWarp(true);
        assertThat(island.hasWarp(), is(true));
        Location reactivated = island.getWarpLocation();
        assertThat(reactivated, is(notNullValue()));
        assertThat(reactivated.getBlockX(), is(10));
        assertThat(reactivated.getBlockY(), is(70));
        assertThat(reactivated.getBlockZ(), is(20));
    }

    @Test
    public void warpStateSurvivesConfigReload() throws Exception {
        IslandInfo island = createIslandInfo();
        island.setWarpLocation(new Location(null, 5, 65, -5, 12.0f, 34.0f));
        island.saveToFile();

        IslandInfo reloaded = new IslandInfo("island", plugin, runtimeConfigs, islandDir());

        assertThat(reloaded.hasWarp(), is(true));
        Location warp = reloaded.getWarpLocation();
        assertThat(warp, is(notNullValue()));
        assertThat(warp.getBlockX(), is(5));
        assertThat(warp.getBlockY(), is(65));
        assertThat(warp.getBlockZ(), is(-5));
        assertThat(warp.getYaw(), is(12.0f));
        assertThat(warp.getPitch(), is(34.0f));
    }

    private IslandInfo createIslandInfo() throws Exception {
        return new IslandInfo("island", plugin, runtimeConfigs, islandDir());
    }

    /**
     * Returns the island directory, seeding a fresh version-3 island config on first use. Once the
     * config exists (e.g. after {@link IslandInfo#saveToFile()}) it is left untouched so a reload
     * reads back the persisted warp state.
     */
    private Path islandDir() throws Exception {
        Path dir = tempDir.resolve("island-info");
        File islandConfigFile = new File(dir.toFile(), "island.yml");
        if (!islandConfigFile.exists()) {
            dir.toFile().mkdirs();
            YamlConfiguration config = new YamlConfiguration();
            config.set("version", 3);
            config.save(islandConfigFile);
        }
        return dir;
    }
}
