package us.talabrek.ultimateskyblock.command.admin.task;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigFactory;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.island.IslandLogic;
import us.talabrek.ultimateskyblock.island.OrphanLogic;
import us.talabrek.ultimateskyblock.message.Msg;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.uuid.PlayerDB;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurgeTaskTest {
    private static final long ABANDONED = Instant.now().minus(Duration.ofDays(90)).toEpochMilli();

    private final List<Player> online = new ArrayList<>();
    private uSkyBlock plugin;
    private IslandLogic islandLogic;
    private PlayerDB playerDB;
    private RuntimeConfigs runtimeConfigs;

    @BeforeEach
    void setUp() throws Exception {
        Server server = BukkitServerMock.setupServerMock();
        I18nUtil.initialize(new File("."), Locale.ENGLISH);
        Msg.configure((sender, message) -> {});
        plugin = mock(uSkyBlock.class);
        islandLogic = mock(IslandLogic.class);
        playerDB = mock(PlayerDB.class);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getIslandLogic()).thenReturn(islandLogic);
        when(plugin.getOrphanLogic()).thenReturn(mock(OrphanLogic.class));
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getOnlinePlayers()).thenAnswer(invocation -> online);
        when(islandLogic.purge(anyString())).thenReturn(true);
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.setDefaults(PluginConfigLoader.loadBundledConfig());
        RuntimeConfig config = new RuntimeConfigFactory(new GameObjectFactory(), Logger.getAnonymousLogger()).load(yaml);
        runtimeConfigs = mock(RuntimeConfigs.class);
        when(runtimeConfigs.current()).thenReturn(config);
    }

    @AfterEach
    void tearDown() {
        Msg.configure(null);
    }

    @Test
    void purgesOnlyTheIslandsThatStillMatchTheScan() {
        island("1,1", 5, ABANDONED);
        UUID cameOnline = island("2,2", 5, ABANDONED);
        online(cameOnline);
        island("3,3", 5, Instant.now().toEpochMilli());
        island("4,4", 40, ABANDONED);
        IslandInfo ignored = mock(IslandInfo.class);
        when(ignored.ignore()).thenReturn(true);
        when(plugin.getIslandInfo("5,5")).thenReturn(ignored);

        PurgeRule rule = new PurgeRule(Instant.now().minus(Duration.ofDays(30)), 25, playerDB);
        new PurgeTask(plugin, runtimeConfigs, new ArrayList<>(List.of("1,1", "2,2", "3,3", "4,4", "5,5")), rule,
            mock(CommandSender.class)).run();

        verify(islandLogic).purge("1,1");
        verify(islandLogic, never()).purge("2,2");
        verify(islandLogic, never()).purge("3,3");
        verify(islandLogic, never()).purge("4,4");
        verify(islandLogic, never()).purge("5,5");
    }

    /** @return its one member, who last played then */
    private UUID island(String name, double level, long lastPlayed) {
        UUID member = UUID.randomUUID();
        IslandInfo island = mock(IslandInfo.class);
        when(island.getMemberUUIDs()).thenReturn(Set.of(member));
        when(island.getLevel()).thenReturn(level);
        when(plugin.getIslandInfo(name)).thenReturn(island);
        OfflinePlayer player = mock(OfflinePlayer.class);
        when(player.getLastPlayed()).thenReturn(lastPlayed);
        when(playerDB.getOfflinePlayer(member)).thenReturn(player);
        return member;
    }

    private void online(UUID uuid) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        online.add(player);
    }
}
