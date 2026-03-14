package us.talabrek.ultimateskyblock.island.task;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.island.IslandGenerator;
import us.talabrek.ultimateskyblock.island.OrphanLogic;
import us.talabrek.ultimateskyblock.message.Msg;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.player.PlayerPerk;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.io.File;
import java.util.Locale;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateIslandTaskTest {
    @Test
    public void stopsIslandSetupWhenSchematicLoadingFails() {
        uSkyBlock plugin = mock(uSkyBlock.class);
        RuntimeConfigs runtimeConfigs = mock(RuntimeConfigs.class);
        Player player = mock(Player.class);
        PlayerPerk playerPerk = mock(PlayerPerk.class);
        PlayerInfo playerInfo = mock(PlayerInfo.class);
        IslandGenerator islandGenerator = mock(IslandGenerator.class);
        OrphanLogic orphanLogic = mock(OrphanLogic.class);
        Scheduler scheduler = mock(Scheduler.class);
        World world = mock(World.class);
        Location next = new Location(world, 100, 120, 100);

        when(playerPerk.getPlayerInfo()).thenReturn(playerInfo);
        when(plugin.getIslandGenerator()).thenReturn(islandGenerator);
        when(plugin.getOrphanLogic()).thenReturn(orphanLogic);
        when(plugin.getScheduler()).thenReturn(scheduler);
        when(islandGenerator.createIsland(next, "default")).thenReturn(false);

        CreateIslandTask task = new CreateIslandTask(plugin, runtimeConfigs, player, playerPerk, next, "default");

        I18nUtil.initialize(new File("."), Locale.ENGLISH);
        Msg.configure((sender, message) -> {});
        try {
            task.run();
        } finally {
            Msg.configure(null);
        }

        verify(playerInfo).setIslandGenerating(false);
        verify(orphanLogic).addOrphan(next);
        verify(scheduler, never()).sync(org.mockito.ArgumentMatchers.any(org.bukkit.scheduler.BukkitRunnable.class),
            org.mockito.ArgumentMatchers.any());
    }
}
