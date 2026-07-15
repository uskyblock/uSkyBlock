package us.talabrek.ultimateskyblock.command.island;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigFactory;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.player.TeleportLogic;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Covers the {@code /is warp <player>} access gate in {@link WarpCommand#doExecute}: a player may
 * warp to another island only when that island has an active warp <em>or</em> the visitor is trusted
 * (WarpCommand ~line 69). The command is trivially constructible ({@code new WarpCommand(plugin,
 * runtimeConfigs)} — no Guice injector), and the target island is a mock, so this stays a focused
 * unit test of the gate rather than of IslandInfo internals.
 */
public class WarpCommandGateTest {
    private uSkyBlock plugin;
    private TeleportLogic teleportLogic;
    private Player player;
    private PlayerInfo targetInfo;
    private IslandInfo targetIsland;
    private WarpCommand command;

    @BeforeEach
    public void setUp() throws Exception {
        BukkitServerMock.setupServerMock();
        I18nUtil.initialize(new File("."), Locale.ENGLISH);

        plugin = mock(uSkyBlock.class);
        RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);

        // RuntimeConfig (and its nested records) cannot be mocked, so build a real one from the
        // bundled defaults plus the overrides RuntimeConfigFactory#load requires. The warp path
        // reads runtimeConfigs.current().protection().visitorWarnOnWarp().
        YamlConfiguration config = new YamlConfiguration();
        config.setDefaults(PluginConfigLoader.loadBundledConfig());
        config.set("language", "en");
        config.set("options.general.worldName", "skyworld");
        config.set("options.general.cooldownRestart", "1m");
        config.set("options.general.biomeChange", "1m");
        config.set("options.general.defaultBiome", "plains");
        config.set("options.general.defaultNetherBiome", "nether_wastes");
        config.set("options.advanced.confirmTimeout", "30s");
        config.set("options.advanced.playerdb.storage", "file");
        config.set("options.advanced.playerdb.nameCache", "maximumSize=100");
        config.set("options.advanced.playerdb.uuidCache", "maximumSize=100");
        config.set("options.advanced.playerCache", "maximumSize=100");
        config.set("options.advanced.islandCache", "maximumSize=100");
        config.set("options.advanced.island.saveEvery", 60);
        config.set("options.advanced.player.saveEvery", 60);
        config.set("options.party.invite-timeout", "1m");
        config.set("options.island.distance", 110);
        config.set("options.island.height", 120);
        config.set("options.island.topTenTimeout", "15m");
        config.set("options.island.islandTeleportDelay", "2s");
        config.set("options.island.chat-format", "default");
        config.set("options.party.chat-format", "default");
        config.set("nether.chunk-generator", "default");
        config.set("plugin-updates.branch", "LATEST");
        doReturn(new RuntimeConfigFactory(new GameObjectFactory(), Logger.getAnonymousLogger()).load(config))
            .when(runtimeConfigs).current();

        teleportLogic = mock(TeleportLogic.class);
        doReturn(teleportLogic).when(plugin).getTeleportLogic();

        player = mock(Player.class);
        doReturn(true).when(player).hasPermission("usb.island.warp");
        doReturn("Visitor").when(player).getDisplayName();

        PlayerInfo senderInfo = mock(PlayerInfo.class);
        doReturn(false).when(senderInfo).isIslandGenerating();
        doReturn(senderInfo).when(plugin).getPlayerInfo(player);

        targetInfo = mock(PlayerInfo.class);
        doReturn(true).when(targetInfo).getHasIsland();
        doReturn(false).when(targetInfo).isIslandGenerating();
        doReturn(targetInfo).when(plugin).getPlayerInfo("target");

        targetIsland = mock(IslandInfo.class);
        doReturn(targetIsland).when(plugin).getIslandInfo(targetInfo);

        command = new WarpCommand(plugin, runtimeConfigs);
    }

    @Test
    public void trustedVisitorMayWarpToIslandWithInactiveWarp() {
        doReturn(false).when(targetIsland).hasWarp();
        doReturn(true).when(targetIsland).isTrusted(player);
        doReturn(false).when(targetIsland).isBanned(player);

        boolean handled = command.execute(player, "island", new HashMap<>(), "target");

        assertThat(handled, is(true));
        verify(teleportLogic).warpTeleport(player, targetInfo, false);
    }

    @Test
    public void untrustedVisitorMayNotWarpToIslandWithInactiveWarp() {
        doReturn(false).when(targetIsland).hasWarp();
        doReturn(false).when(targetIsland).isTrusted(player);

        boolean handled = command.execute(player, "island", new HashMap<>(), "target");

        assertThat(handled, is(true));
        verify(teleportLogic, never()).warpTeleport(any(), any(), anyBoolean());
    }
}
