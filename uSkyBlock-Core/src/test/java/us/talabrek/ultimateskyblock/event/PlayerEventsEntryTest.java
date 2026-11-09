package us.talabrek.ultimateskyblock.event;

import dk.lockfuglsang.minecraft.util.TimeUtil;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.Scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The entries that apply a clear marked for the player's next entry. */
public class PlayerEventsEntryTest {
    private uSkyBlock plugin;
    private Scheduler scheduler;
    private PlayerEvents listener;
    private Player player;

    @BeforeEach
    public void setUp() {
        plugin = mock(uSkyBlock.class);
        scheduler = mock(Scheduler.class);
        when(plugin.getScheduler()).thenReturn(scheduler);
        player = mock(Player.class);
        listener = new PlayerEvents(plugin, mock(RuntimeConfigs.class));
    }

    @Test
    public void aWorldChangeAppliesTheClear() {
        listener.onWorldChanged(new PlayerChangedWorldEvent(player, mock(World.class)));

        verify(plugin).clearInventoryOnEntry(player);
    }

    @Test
    public void aJoinAppliesTheClearATickLater() {
        listener.onJoin(new PlayerJoinEvent(player, "joined"));

        ArgumentCaptor<Runnable> later = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).sync(later.capture(), eq(TimeUtil.ticksAsDuration(1)));
        verify(plugin, never()).clearInventoryOnEntry(any());
        later.getValue().run();
        verify(plugin).clearInventoryOnEntry(player);
    }
}
