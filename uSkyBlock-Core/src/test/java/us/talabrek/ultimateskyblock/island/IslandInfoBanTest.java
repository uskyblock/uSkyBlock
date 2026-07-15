package us.talabrek.ultimateskyblock.island;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.player.PlayerLogic;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.uuid.PlayerDB;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Regression test for the single-argument {@link IslandInfo#banPlayer(OfflinePlayer)} overload,
 * which previously delegated to {@code trustPlayer(target, null)} and therefore trusted the target
 * instead of banning them. It is a public API method (api.IslandInfo), so third-party callers were
 * affected. The real IslandInfo is built over a temp-dir YAML, following IslandInfoLogFormatTest.
 */
public class IslandInfoBanTest {
    private static final UUID LEADER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TARGET_UUID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @TempDir
    Path tempDir;

    private uSkyBlock plugin;
    private RuntimeConfigs runtimeConfigs;

    @BeforeEach
    public void setUp() throws Exception {
        BukkitServerMock.setupServerMock();
        I18nUtil.initialize(new File("."), Locale.ENGLISH);

        plugin = mock(uSkyBlock.class);
        doReturn(mock(PlayerDB.class)).when(plugin).getPlayerDB();
        // banPlayerInfo looks the target up via PlayerLogic; a mock returns null -> no-op.
        doReturn(mock(PlayerLogic.class)).when(plugin).getPlayerLogic();

        // banPlayer fires an IslandBanPlayerEvent through the plugin manager; a mock makes it a no-op.
        PluginManager pluginManager = mock(PluginManager.class);
        Server server = mock(Server.class);
        doReturn(pluginManager).when(server).getPluginManager();
        doReturn(server).when(plugin).getServer();

        runtimeConfigs = mock(RuntimeConfigs.class);
    }

    @Test
    public void singleArgBanPlayerBansRatherThanTrusts() throws Exception {
        OfflinePlayer target = mock(OfflinePlayer.class);
        doReturn(TARGET_UUID).when(target).getUniqueId();

        IslandInfo island = createSoloIsland();

        assertTrue(island.banPlayer(target), "single-arg banPlayer should ban and return true");
        assertTrue(island.isBanned(target), "target should be banned");
        assertFalse(island.isTrusted(target), "target must NOT be trusted by banPlayer");
    }

    private IslandInfo createSoloIsland() throws Exception {
        File islandDir = tempDir.resolve("islands").toFile();
        islandDir.mkdirs();
        YamlConfiguration config = new YamlConfiguration();
        config.set("version", 3);
        config.set("party.leader", "Leader");
        config.set("party.leader-uuid", LEADER_UUID.toString());
        config.set("party.members." + LEADER_UUID + ".name", "Leader");
        config.set("party.currentSize", 1);
        config.save(new File(islandDir, "island.yml"));
        return new IslandInfo("island", plugin, runtimeConfigs, islandDir.toPath());
    }
}
