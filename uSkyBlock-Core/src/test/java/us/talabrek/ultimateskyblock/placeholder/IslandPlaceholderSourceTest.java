package us.talabrek.ultimateskyblock.placeholder;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.api.IslandRank;
import us.talabrek.ultimateskyblock.island.IslandLogic;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.island.LimitLogic;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.player.PlayerLogic;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static us.talabrek.ultimateskyblock.message.Msg.plainText;

public class IslandPlaceholderSourceTest {

    private static final UUID PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private Server server;
    private IslandPlaceholderSource source;
    private IslandInfo islandInfo;
    private OfflinePlayer player;

    @BeforeEach
    public void setUp() throws Exception {
        server = BukkitServerMock.setupServerMock();
        when(server.isPrimaryThread()).thenReturn(true);
        I18nUtil.initialize(new File("."), Locale.ENGLISH);

        uSkyBlock plugin = mock(uSkyBlock.class);
        PluginDescriptionFile description = mock(PluginDescriptionFile.class);
        when(description.getVersion()).thenReturn("3.5.0-test");
        when(plugin.getDescription()).thenReturn(description);

        PlayerLogic playerLogic = mock(PlayerLogic.class);
        IslandLogic islandLogic = mock(IslandLogic.class);
        PlayerInfo playerInfo = mock(PlayerInfo.class);
        // Mock the concrete class (island.IslandInfo implements api.IslandInfo)
        islandInfo = mock(IslandInfo.class);
        when(playerLogic.getPlayerInfo(PLAYER_ID)).thenReturn(playerInfo);
        when(islandLogic.getIslandInfo(playerInfo)).thenReturn(islandInfo);
        when(islandInfo.getName()).thenReturn("islandA");
        // IslandRank constructor: (String islandName, String leaderName, List<String> members, double score, int rank)
        when(islandLogic.getRank("islandA")).thenReturn(new IslandRank("islandA", "leader", List.of(), 120.5d, 7));

        CreatureCountSnapshots snapshots = mock(CreatureCountSnapshots.class);
        when(snapshots.counts(islandInfo)).thenReturn(Map.of(LimitLogic.CreatureType.MONSTER, 4));

        player = mock(OfflinePlayer.class);
        when(player.getUniqueId()).thenReturn(PLAYER_ID);

        source = new IslandPlaceholderSource(plugin, playerLogic, islandLogic, snapshots);
    }

    @Test
    public void resolvesVersion() {
        assertThat(plainText(source.resolve(player, "version")), is("3.5.0-test"));
    }

    @Test
    public void resolvesFormattedLevel() {
        // the double literal 120.55 is actually 120.5499..., which would round to 120.5
        when(islandInfo.getLevel()).thenReturn(120.56d);
        assertThat(plainText(source.resolve(player, "island_level")), is("120.6"));
        assertThat(plainText(source.resolve(player, "island_level_int")), is("121"));
    }

    @Test
    public void resolvesRank() {
        assertThat(plainText(source.resolve(player, "island_rank")), is("7"));
    }

    @Test
    public void unrankedIslandFallsBackToNotAvailable() {
        IslandLogic unranked = mockIslandLogic();
        when(unranked.getRank("islandA")).thenReturn(null);
        IslandPlaceholderSource unrankedSource = new IslandPlaceholderSource(
            mock(uSkyBlock.class), mockPlayerLogic(), unranked, mock(CreatureCountSnapshots.class));

        assertThat(plainText(unrankedSource.resolve(player, "island_rank")), is("N/A"));
    }

    @Test
    public void resolvesCreatureCountFromSnapshots() {
        assertThat(plainText(source.resolve(player, "island_monsters")), is("4"));
    }

    @Test
    public void unavailableSnapshotRendersEllipsis() {
        CreatureCountSnapshots cold = mock(CreatureCountSnapshots.class);
        when(cold.counts(islandInfo)).thenReturn(null);
        IslandPlaceholderSource coldSource = new IslandPlaceholderSource(
            mock(uSkyBlock.class), mockPlayerLogic(), mockIslandLogic(), cold);

        assertThat(plainText(coldSource.resolve(player, "island_monsters")), is("…"));
    }

    @Test
    public void returnsNotAvailableWithoutIsland() {
        PlayerLogic noIsland = mock(PlayerLogic.class);
        when(noIsland.getPlayerInfo(PLAYER_ID)).thenReturn(null);
        IslandPlaceholderSource islandless = new IslandPlaceholderSource(
            mock(uSkyBlock.class), noIsland, mock(IslandLogic.class), mock(CreatureCountSnapshots.class));

        assertThat(plainText(islandless.resolve(player, "island_level")), is("N/A"));
    }

    @Test
    public void unknownKeyResolvesNull() {
        assertNull(source.resolve(player, "bogus_key"));
        assertNull(source.resolve(player, "usb_island_level")); // old prefixed names are NOT keys
    }

    @Test
    public void keysContainAll26() {
        assertThat(source.keys().size(), is(26));
        assertThat(source.keys().contains("island_copper_golems_max"), is(true));
        assertThat(source.keys().contains("version"), is(true));
    }

    private PlayerLogic mockPlayerLogic() {
        PlayerLogic logic = mock(PlayerLogic.class);
        PlayerInfo info = mock(PlayerInfo.class);
        when(logic.getPlayerInfo(PLAYER_ID)).thenReturn(info);
        return logic;
    }

    private IslandLogic mockIslandLogic() {
        IslandLogic logic = mock(IslandLogic.class);
        when(logic.getIslandInfo(any(PlayerInfo.class))).thenReturn(islandInfo);
        return logic;
    }
}
