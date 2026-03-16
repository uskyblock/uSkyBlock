package us.talabrek.ultimateskyblock;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import us.talabrek.ultimateskyblock.island.IslandLogic;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static dk.lockfuglsang.minecraft.util.FormatUtil.stripFormatting;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

public class uSkyBlockTest {

    @Test
    public void testStripFormatting() throws Exception {
        String text = "&eHello \u00a7bBabe &l&kYou wanna dance&r with somebody";

        assertThat(stripFormatting(text), is("Hello Babe You wanna dance with somebody"));
    }

    @Test
    public void restartPlayerIslandDoesNotClearIslandWhenSchemeValidationFails() throws Exception {
        uSkyBlock plugin = mock(uSkyBlock.class, Answers.CALLS_REAL_METHODS);
        Player player = mock(Player.class);
        IslandLogic islandLogic = mock(IslandLogic.class);
        setField(plugin, "islandLogic", islandLogic);
        doReturn(false).when(plugin).validateIslandScheme(player, "broken");

        boolean restarted = plugin.restartPlayerIsland(player, new Location(mock(World.class), 0, 64, 0), "broken");

        assertFalse(restarted);
        verifyNoInteractions(islandLogic);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        var field = uSkyBlock.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
