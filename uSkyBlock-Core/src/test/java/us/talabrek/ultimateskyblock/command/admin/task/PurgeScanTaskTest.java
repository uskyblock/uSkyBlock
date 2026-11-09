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
import org.junit.jupiter.api.io.TempDir;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigFactory;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.message.Msg;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.uuid.PlayerDB;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PurgeScanTaskTest {
    @TempDir
    Path islandDir;
    private final List<Player> online = new ArrayList<>();
    private uSkyBlock plugin;
    private PlayerDB playerDB;
    private RuntimeConfigs runtimeConfigs;

    @BeforeEach
    void setUp() throws Exception {
        Server server = BukkitServerMock.setupServerMock();
        I18nUtil.initialize(new File("."), Locale.ENGLISH);
        Msg.configure((sender, message) -> {});
        plugin = mock(uSkyBlock.class);
        playerDB = mock(PlayerDB.class);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getPlayerDB()).thenReturn(playerDB);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getOnlinePlayers()).thenAnswer(invocation -> online);
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
    void keepsTheIslandOfAMemberWhoIsOnline() throws Exception {
        UUID firstSession = island("1,1", 0);
        UUID back = island("2,2", Instant.now().minus(Duration.ofDays(90)).toEpochMilli());
        island("3,3", Instant.now().minus(Duration.ofDays(90)).toEpochMilli());
        online(firstSession);
        online(back);

        PurgeScanTask task = new PurgeScanTask(plugin, runtimeConfigs, islandDir.toFile(), Duration.ofDays(30), 25,
            mock(CommandSender.class), () -> {});
        task.run();

        assertEquals(List.of("3,3"), task.getPurgeList());
    }

    /** @return its one member, who last played then */
    private UUID island(String name, long lastPlayed) throws IOException {
        Files.createFile(islandDir.resolve(name + ".yml"));
        UUID member = UUID.randomUUID();
        IslandInfo island = mock(IslandInfo.class);
        when(island.getMemberUUIDs()).thenReturn(Set.of(member));
        when(island.getLevel()).thenReturn(5.0);
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
