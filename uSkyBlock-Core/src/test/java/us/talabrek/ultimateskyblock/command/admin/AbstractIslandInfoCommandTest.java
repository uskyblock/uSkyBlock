package us.talabrek.ultimateskyblock.command.admin;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import us.talabrek.ultimateskyblock.command.island.MobLimitCommand;
import us.talabrek.ultimateskyblock.handler.ConfirmHandler;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.island.LimitLogic;
import us.talabrek.ultimateskyblock.message.Msg;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The island commands given a player name, such as {@code /usb island info <player>}, and their fallback to the island
 * the sender stands on.
 */
public class AbstractIslandInfoCommandTest {
    private final uSkyBlock plugin = mock(uSkyBlock.class);
    private final LimitLogic limitLogic = mock(LimitLogic.class);
    private final CommandSender sender = mock(CommandSender.class);
    private final PlayerInfo playerInfo = mock(PlayerInfo.class);
    private final IslandInfo islandInfo = mock(IslandInfo.class);
    private final List<String> sent = new ArrayList<>();
    private MockedStatic<uSkyBlock> instance;

    @BeforeEach
    public void setUp() {
        I18nUtil.initialize(new File("."), Locale.ENGLISH);
        Msg.configure((recipient, message) -> sent.add(Msg.plainText(message)));
        instance = mockStatic(uSkyBlock.class);
        instance.when(uSkyBlock::getInstance).thenReturn(plugin);
        when(sender.hasPermission(anyString())).thenReturn(true);
        when(plugin.getPlayerInfo("Owner")).thenReturn(playerInfo);
        when(plugin.getIslandInfo(playerInfo)).thenReturn(islandInfo);
        when(plugin.getLimitLogic()).thenReturn(limitLogic);
        when(playerInfo.getPlayerName()).thenReturn("Owner");
        when(islandInfo.getName()).thenReturn("128,0");
        when(islandInfo.asComponentLines()).thenReturn(new Component[]{Component.text("Island 128,0 of Owner")});
        when(limitLogic.getSummaryComponents(islandInfo)).thenReturn(List.of(Component.text("Limits of 128,0")));
    }

    @AfterEach
    public void tearDown() {
        instance.close();
        Msg.configure(null);
    }

    @Test
    public void infoShowsTheIslandOfThePlayer() {
        adminIslandCommand().execute(sender, "island", new HashMap<>(), "info", "Owner");

        assertEquals(List.of("Island 128,0 of Owner"), sent);
    }

    @Test
    public void getShowsAFieldOfTheIslandOfThePlayer() {
        adminIslandCommand().execute(sender, "island", new HashMap<>(), "get", "Owner", "name");

        assertEquals(List.of("Current value for name is '128,0'."), sent);
    }

    @Test
    public void setSetsAFieldOfTheIslandOfThePlayer() {
        adminIslandCommand().execute(sender, "island", new HashMap<>(), "set", "Owner", "ignore", "true");

        verify(islandInfo).setIgnore(true);
        assertEquals(List.of("ignore was set to 'true'."), sent);
    }

    @Test
    public void limitsShowsTheLimitsOfTheIslandOfThePlayer() {
        assertTrue(new MobLimitCommand(plugin).execute(sender, "limits", new HashMap<>(), "Owner"));

        assertEquals(List.of("Limits of 128,0"), sent);
    }

    @Test
    public void saysThatThereIsNoSuchPlayer() {
        assertFalse(new MobLimitCommand(plugin).execute(sender, "limits", new HashMap<>(), "Nobody"));

        assertEquals(List.of("Invalid player Nobody supplied."), sent);
        verify(limitLogic, never()).getSummaryComponents(any());
    }

    @Test
    public void saysThatThePlayerHasNoIsland() {
        when(plugin.getIslandInfo(playerInfo)).thenReturn(null);

        assertFalse(new MobLimitCommand(plugin).execute(sender, "limits", new HashMap<>(), "Owner"));

        assertEquals(List.of("Player Owner has no island."), sent);
        verify(limitLogic, never()).getSummaryComponents(any());
    }

    @Test
    public void withoutAPlayerNameTakesTheIslandThePlayerStandsOn() {
        Player player = mock(Player.class);
        Location location = mock(Location.class);
        when(player.getLocation()).thenReturn(location);
        when(plugin.getIslandInfo("128,0")).thenReturn(islandInfo);

        try (MockedStatic<WorldGuardHandler> worldGuard = mockStatic(WorldGuardHandler.class)) {
            worldGuard.when(() -> WorldGuardHandler.getIslandNameAt(location)).thenReturn("128,0");
            assertTrue(new MobLimitCommand(plugin).execute(player, "limits", new HashMap<>()));
        }

        assertEquals(List.of("Limits of 128,0"), sent);
    }

    private AdminIslandCommand adminIslandCommand() {
        return new AdminIslandCommand(plugin, mock(ConfirmHandler.class));
    }
}
