package us.talabrek.ultimateskyblock.command;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.api.event.AcceptEvent;
import us.talabrek.ultimateskyblock.api.event.InviteEvent;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.ItemStackSpec;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.message.Msg;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InviteHandlerTest {

    private uSkyBlock plugin;
    private Scheduler scheduler;
    private RuntimeConfigs runtimeConfigs;
    private InviteHandler handler;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        BukkitServerMock.setupServerMock();
        I18nUtil.initialize(new File("."), Locale.ENGLISH);
        Msg.configure((sender, message) -> {});

        plugin = mock(uSkyBlock.class);
        scheduler = mock(Scheduler.class);
        runtimeConfigs = mock(RuntimeConfigs.class);

        when(runtimeConfigs.current()).thenReturn(runtimeConfig());

        handler = new InviteHandler(plugin, scheduler, runtimeConfigs);
    }

    @AfterEach
    void tearDown() {
        Msg.configure(null);
    }

    @Test
    void acceptWithNoInviteDoesNothing() {
        Player player = mockPlayer();
        when(plugin.getIslandInfo(player)).thenReturn(null);

        AcceptEvent event = new AcceptEvent(player);
        handler.onAcceptEvent(event);

        verify(plugin, never()).deletePlayerIsland(any(), any());
    }

    @Test
    void acceptBlockedWhenPlayerIsInParty() {
        Player player = mockPlayer();
        PlayerInfo playerInfo = mock(PlayerInfo.class);
        when(playerInfo.getHasIsland()).thenReturn(false);
        when(plugin.getPlayerInfo(player)).thenReturn(playerInfo);

        IslandInfo existingIsland = mock(IslandInfo.class);
        when(existingIsland.isParty()).thenReturn(true);
        when(plugin.getIslandInfo(player)).thenReturn(existingIsland);

        sendInviteFor(player);

        AcceptEvent event = new AcceptEvent(player);
        handler.onAcceptEvent(event);

        verify(plugin, never()).deletePlayerIsland(any(), any());
    }

    @Test
    void acceptWithoutIslandDoesNotRequireConfirmation() {
        Player player = mockPlayer();
        PlayerInfo playerInfo = mock(PlayerInfo.class);
        when(playerInfo.getHasIsland()).thenReturn(false);
        when(plugin.getPlayerInfo(player)).thenReturn(playerInfo);
        when(plugin.getIslandInfo(player)).thenReturn(null);

        IslandInfo targetIsland = mock(IslandInfo.class);
        when(targetIsland.getName()).thenReturn("target-island");
        when(plugin.getIslandInfo("target-island")).thenReturn(targetIsland);
        when(plugin.getTeleportLogic()).thenReturn(mock(us.talabrek.ultimateskyblock.player.TeleportLogic.class));

        sendInviteFor(player, targetIsland);

        AcceptEvent event = new AcceptEvent(player);
        handler.onAcceptEvent(event);

        verify(targetIsland).addMember(playerInfo);
    }

    private Player mockPlayer() {
        Player player = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn("TestPlayer");
        when(player.getDisplayName()).thenReturn("TestPlayer");
        return player;
    }

    private void sendInviteFor(Player guest) {
        IslandInfo island = mock(IslandInfo.class);
        when(island.getName()).thenReturn("test-island");
        sendInviteFor(guest, island);
    }

    private void sendInviteFor(Player guest, IslandInfo island) {
        Player inviter = mockPlayer();
        when(island.getMaxPartySize()).thenReturn(4);
        when(island.getPartySize()).thenReturn(1);
        when(scheduler.async(any(Runnable.class), any(Duration.class))).thenReturn(mock(BukkitTask.class));

        InviteEvent inviteEvent = new InviteEvent(inviter, island, guest);
        handler.onInviteEvent(inviteEvent);
    }

    private static RuntimeConfig runtimeConfig() {
        ItemStackSpec tool = new ItemStackSpec(new ItemStack(Material.STONE));
        return new RuntimeConfig(
            "en",
            Locale.ENGLISH,
            new RuntimeConfig.Init(Duration.ZERO),
            new RuntimeConfig.General(4, "skyworld", Duration.ZERO, Duration.ZERO, Duration.ZERO, "plains", "nether_wastes", 64, Duration.ZERO),
            new RuntimeConfig.Island(
                128, 150, false, 128, 64, List.of(), true, Map.of(), true, true, true, "default",
                Duration.ZERO, Duration.ZERO, false, Duration.ZERO, 0.5d, Duration.ZERO, true, 10, false, "",
                new RuntimeConfig.SpawnLimits(true, 64, 50, 16, 5, 0), Map.of()
            ),
            new RuntimeConfig.Extras(false, true, true),
            new RuntimeConfig.Protection(true, true, true, true, true, true, true, true, true, true, true, true, false, false, true, true, true, false, false, false, true),
            new RuntimeConfig.Nether(false, 7, 75, "", new RuntimeConfig.Terraform(false, 0d, 0d, 0, Map.of(), Map.of()), new RuntimeConfig.SpawnChances(false, 0d, 0d, 0d)),
            new RuntimeConfig.Restart(true, true, true, true, false, true, Duration.ZERO, List.of()),
            new RuntimeConfig.Advanced(Duration.ZERO, false, 0d, true, "", "", "", "", Duration.ZERO, Duration.ZERO, "", 4, Duration.ZERO, 0d, Duration.ZERO, null,
                new RuntimeConfig.PlayerDb("bukkit", "", "", Duration.ZERO)),
            new RuntimeConfig.Async(Duration.ZERO, 0L, Duration.ZERO),
            new RuntimeConfig.AsyncWorldEdit(false, Duration.ZERO, Duration.ZERO),
            new RuntimeConfig.Party(Duration.ofMinutes(5), "", List.of(), List.of(), Map.of()),
            new RuntimeConfig.PluginUpdates(true, "RELEASE"),
            new RuntimeConfig.Spawning(new RuntimeConfig.Guardians(false, 0, 0d), new RuntimeConfig.Phantoms(true, false)),
            new RuntimeConfig.Placeholder(false, false, false),
            new RuntimeConfig.ToolMenu(false, tool, List.of()),
            new RuntimeConfig.Signs(true),
            new RuntimeConfig.WorldGuard(true, true),
            new RuntimeConfig.Importer(0.1d, Duration.ZERO),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of()
        );
    }
}
