package us.talabrek.ultimateskyblock.event;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.api.async.Callback;
import us.talabrek.ultimateskyblock.api.event.CreateIslandEvent;
import us.talabrek.ultimateskyblock.api.event.IslandInfoEvent;
import us.talabrek.ultimateskyblock.api.event.MemberJoinedEvent;
import us.talabrek.ultimateskyblock.api.event.MemberLeftEvent;
import us.talabrek.ultimateskyblock.api.event.RestartIslandEvent;
import us.talabrek.ultimateskyblock.api.event.uSkyBlockScoreChangedEvent;
import us.talabrek.ultimateskyblock.config.PluginConfigLoader;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigFactory;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.island.BlockLimitLogic;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.island.level.IslandScore;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.LocationUtil;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class InternalEventsTest {
    private BlockLimitLogic fakeBlockLimitLogic;
    private uSkyBlock fakePlugin;
    private InternalEvents internalEvents;

    @BeforeEach
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        BukkitServerMock.setupServerMock();
        fakePlugin = mock(uSkyBlock.class);

        YamlConfiguration config = new YamlConfiguration();
        config.setDefaults(PluginConfigLoader.loadBundledConfig());
        config.set("options.party.join-commands", Arrays.asList("lets", "test", "this"));
        config.set("options.party.leave-commands", Arrays.asList("dont", "stop", "me", "now"));
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
        doReturn(config).when(fakePlugin).getConfig();

        RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
        doReturn(new RuntimeConfigFactory(new GameObjectFactory()).load(config)).when(runtimeConfigs).current();
        internalEvents = new InternalEvents(fakePlugin, runtimeConfigs);

        fakeBlockLimitLogic = mock(BlockLimitLogic.class);
        doNothing().when(fakeBlockLimitLogic).updateBlockCount(any(), any());
        doReturn(fakeBlockLimitLogic).when(fakePlugin).getBlockLimitLogic();

        doReturn(true).when(fakePlugin).restartPlayerIsland(any(), any(), any());
        doNothing().when(fakePlugin).createIsland(any(), any());
        doNothing().when(fakePlugin).calculateScoreAsync(any(), any(), any());
    }

    @Test
    public void testOnRestart() {
        Player player = getFakePlayer();
        Location island = new Location(null, 1.00, 2.00, -1.00);
        String schematic = "default";

        RestartIslandEvent event = new RestartIslandEvent(player, island, schematic);
        internalEvents.onRestart(event);
        verify(fakePlugin).restartPlayerIsland(player, island, schematic);
    }

    @Test
    public void testOnCreate() {
        Player player = getFakePlayer();
        String schematic = "default";

        CreateIslandEvent event = new CreateIslandEvent(player, schematic);
        internalEvents.onCreate(event);
        verify(fakePlugin).createIsland(player, schematic);
    }

    @Test
    public void testOnMemberJoin() {
        IslandInfo fakeIslandInfo = mock(IslandInfo.class);
        PlayerInfo fakePlayerInfo = mock(PlayerInfo.class);
        doReturn(true).when(fakePlayerInfo).execCommands(any());

        List<String> commandList = fakePlugin.getConfig().getStringList("options.party.join-commands");

        MemberJoinedEvent event = new MemberJoinedEvent(fakeIslandInfo, fakePlayerInfo);
        internalEvents.onMemberJoin(event);
        verify(fakePlayerInfo).execCommands(commandList);
    }

    @Test
    public void testOnMemberLeft() {
        IslandInfo fakeIslandInfo = mock(IslandInfo.class);
        PlayerInfo fakePlayerInfo = mock(PlayerInfo.class);
        doReturn(true).when(fakePlayerInfo).execCommands(any());

        List<String> commandList = fakePlugin.getConfig().getStringList("options.party.leave-commands");

        MemberLeftEvent event = new MemberLeftEvent(fakeIslandInfo, fakePlayerInfo);
        internalEvents.onMemberLeft(event);
        verify(fakePlayerInfo).execCommands(commandList);
    }

    @Test
    public void testOnScoreChanged() {
        Player fakePlayer = getFakePlayer();
        IslandScore fakeIslandScore = mock(IslandScore.class);
        Location islandLocation = new Location(null, -10.00, 25.00, 10.00);

        uSkyBlockScoreChangedEvent event = new uSkyBlockScoreChangedEvent(fakePlayer, fakePlugin,
            fakeIslandScore, islandLocation);
        internalEvents.onScoreChanged(event);
        verify(fakeBlockLimitLogic).updateBlockCount(islandLocation, fakeIslandScore);
    }

    @Test
    public void testOnInfoEvent() {
        Player fakePlayer = getFakePlayer();
        Location islandLocation = new Location(null, -10.00, 25.00, 10.00);
        Callback<us.talabrek.ultimateskyblock.api.model.IslandScore> callback =
            new Callback<>() {
                @Override
                public void run() {
                    // Do nothing
                }
            };

        IslandInfoEvent event = new IslandInfoEvent(fakePlayer, islandLocation, callback);
        internalEvents.onInfoEvent(event);
        verify(fakePlugin).calculateScoreAsync(fakePlayer, LocationUtil.getIslandName(islandLocation), callback);
    }

    private Player getFakePlayer() {
        Player fakePlayer = mock(Player.class);
        when(fakePlayer.getUniqueId()).thenReturn(UUID.fromString("29292160-6d49-47a3-ae1c-7c800e14cca3"));
        when(fakePlayer.getName()).thenReturn("linksssofrechts");
        return fakePlayer;
    }
}
