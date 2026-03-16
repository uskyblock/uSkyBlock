package us.talabrek.ultimateskyblock.command.island;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.ItemStackSpec;
import us.talabrek.ultimateskyblock.handler.ConfirmHandler;
import us.talabrek.ultimateskyblock.handler.CooldownHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.message.Msg;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RestartCommandTest {
    @Test
    public void doesNotResetCooldownOrFireEventWhenSchemeIsUnavailable() {
        uSkyBlock plugin = mock(uSkyBlock.class);
        RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
        CooldownHandler cooldownHandler = mock(CooldownHandler.class);
        ConfirmHandler confirmHandler = mock(ConfirmHandler.class);
        Player player = mock(Player.class);
        PlayerInfo playerInfo = mock(PlayerInfo.class);
        IslandInfo island = mock(IslandInfo.class);

        when(runtimeConfigs.current()).thenReturn(runtimeConfig(false));
        when(plugin.getCooldownHandler()).thenReturn(cooldownHandler);
        when(cooldownHandler.getCooldown(player, "restart")).thenReturn(Duration.ZERO);
        when(plugin.getConfirmHandler()).thenReturn(confirmHandler);
        when(confirmHandler.checkCommand(player, "/is restart")).thenReturn(true);
        when(plugin.validateIslandScheme(player, "broken")).thenReturn(false);
        when(island.getPartySize()).thenReturn(1);
        when(island.getSchematicName()).thenReturn("broken");
        when(playerInfo.isIslandGenerating()).thenReturn(false);

        I18nUtil.initialize(new File("."), Locale.ENGLISH);
        Msg.configure((sender, message) -> {});
        try {
            RestartCommand command = new RestartCommand(plugin, runtimeConfigs);
            command.doExecute("restart", player, playerInfo, island, Map.of());
        } finally {
            Msg.configure(null);
        }

        verify(cooldownHandler, never()).resetCooldown(eq(player), eq("restart"), any());
        verify(plugin, never()).getServer();
    }

    private static RuntimeConfig runtimeConfig(boolean schemesEnabled) {
        ItemStackSpec tool = new ItemStackSpec(new ItemStack(Material.STONE));
        return new RuntimeConfig(
            "en",
            Locale.ENGLISH,
            new RuntimeConfig.Init(Duration.ZERO),
            new RuntimeConfig.General(4, "skyworld", Duration.ZERO, Duration.ZERO, Duration.ZERO, "plains", "nether_wastes", 64, Duration.ZERO),
            new RuntimeConfig.Island(
                128, 150, false, 128, 64, List.of(), true, Map.of(), true, true, true, "default",
                Duration.ZERO, Duration.ZERO, false, Duration.ZERO, 0.5d, Duration.ZERO, true, 10, schemesEnabled, "",
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
            new RuntimeConfig.Party(Duration.ZERO, "", List.of(), List.of(), Map.of()),
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
