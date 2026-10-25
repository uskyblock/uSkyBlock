package us.talabrek.ultimateskyblock.util;

import dk.lockfuglsang.minecraft.util.FormatUtil;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.SkyUpdateChecker;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.island.IslandLogic;
import us.talabrek.ultimateskyblock.player.PlayerLogic;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PluginInfoTest {

    private final SkyUpdateChecker updateChecker = mock(SkyUpdateChecker.class);
    private final PluginInfo pluginInfo = new PluginInfo(mock(Plugin.class), mock(IslandLogic.class),
        mock(RuntimeConfigs.class), updateChecker, mock(PlayerLogic.class), Logger.getLogger("PluginInfoTest"));

    @Test
    public void noNoticeBeforeAnyFailedCheck() {
        assertEquals("", pluginInfo.updateNotice());
    }

    @Test
    public void manualCheckHintAfterFailedCheck() {
        when(updateChecker.lastCheckFailed()).thenReturn(true);

        assertEquals("Failed to check for new uSkyBlock versions. "
            + "Please check for updates manually at https://uskyblock.github.io/\n", plainNotice());
    }

    @Test
    public void availableUpdateIsAnnounced() {
        when(updateChecker.isUpdateAvailable()).thenReturn(true);
        when(updateChecker.getLatestVersion()).thenReturn("3.8.0");

        assertEquals("A new update of uSkyBlock is available: 3.8.0\n"
            + "Visit https://uskyblock.github.io/ to download.\n", plainNotice());
    }

    private String plainNotice() {
        return FormatUtil.stripFormatting(pluginInfo.updateNotice());
    }
}
