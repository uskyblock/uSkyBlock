package us.talabrek.ultimateskyblock.util;

import dk.lockfuglsang.minecraft.util.FormatUtil;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.SkyUpdateChecker;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.island.IslandLogic;
import us.talabrek.ultimateskyblock.player.PlayerLogic;
import us.talabrek.ultimateskyblock.update.ModrinthVersion;
import us.talabrek.ultimateskyblock.update.ServerPlatform;
import us.talabrek.ultimateskyblock.update.Updates;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PluginInfoTest {

    private final SkyUpdateChecker updateChecker = mock(SkyUpdateChecker.class);
    private final PluginInfo pluginInfo = new PluginInfo(mock(Plugin.class), mock(IslandLogic.class),
        mock(RuntimeConfigs.class), updateChecker, mock(PlayerLogic.class), Logger.getLogger("PluginInfoTest"));

    private void found(String... versions) {
        List<ModrinthVersion> listed = Arrays.stream(versions)
            .map(version -> new ModrinthVersion(version, "release", Instant.EPOCH, List.of("26.3"),
                List.of("paper", "spigot"), null))
            .toList();
        when(updateChecker.getUpdates()).thenReturn(Updates.of(listed, "RELEASE", "3.7.1", ServerPlatform.spigot("26.3")));
    }

    @Test
    public void noNoticeBeforeAnyCheck() {
        assertEquals("", pluginInfo.updateNotice());
    }

    @Test
    public void manualCheckHintAfterFailedCheck() {
        when(updateChecker.lastCheckFailed()).thenReturn(true);

        assertEquals("Failed to check for new uSkyBlock versions. "
            + "Please check for updates manually at https://modrinth.com/plugin/uskyblock\n", plainNotice());
    }

    @Test
    public void availableUpdateIsAnnounced() {
        found("3.8.0", "3.7.1");

        assertEquals("A new update of uSkyBlock is available: 3.8.0\n"
            + "Run /usb update to stage it for the next restart, or download it from "
            + "https://modrinth.com/plugin/uskyblock/version/3.8.0\n", plainNotice());
    }

    @Test
    public void newMajorVersionIsAnnouncedWithItsRequirement() {
        found("4.0.0", "3.7.1");

        assertEquals("A new major version of uSkyBlock is available: 4.0.0 (needs Minecraft 26.3)\n"
            + "Read the upgrade guide before upgrading: https://uskyblock.org\n",
            plainNotice());
    }

    @Test
    public void updateAndNewMajorVersionAreBothAnnounced() {
        found("4.0.0", "3.7.2");

        assertEquals("A new update of uSkyBlock is available: 3.7.2\n"
            + "Run /usb update to stage it for the next restart, or download it from "
            + "https://modrinth.com/plugin/uskyblock/version/3.7.2\n"
            + "A new major version of uSkyBlock is available: 4.0.0 (needs Minecraft 26.3)\n"
            + "Read the upgrade guide before upgrading: https://uskyblock.org\n",
            plainNotice());
    }

    private String plainNotice() {
        return FormatUtil.stripFormatting(pluginInfo.updateNotice());
    }
}
