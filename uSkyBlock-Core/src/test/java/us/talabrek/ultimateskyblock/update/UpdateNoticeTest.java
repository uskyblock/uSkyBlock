package us.talabrek.ultimateskyblock.update;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import us.talabrek.ultimateskyblock.SkyUpdateChecker;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.message.Msg;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdateNoticeTest {

    private static final String GUIDE = "https://uskyblock.github.io/";

    @TempDir
    Path plugins;

    private final SkyUpdateChecker updateChecker = mock(SkyUpdateChecker.class);
    private final UpdateStager stager = mock(UpdateStager.class);
    private RuntimeConfigs runtimeConfigs;
    private UpdateServer server;
    private UpdateNotice notice;

    @BeforeEach
    public void setUp() {
        I18nUtil.initialize(new File("."), Locale.ENGLISH);
        server = new UpdateServer(plugins, "3.7.1");
        notice(true);
    }

    private void notice(boolean checkEnabled) {
        notice(checkEnabled, false);
    }

    private void notice(boolean checkEnabled, boolean autoStage) {
        runtimeConfigs = UpdateServer.config(checkEnabled, "RELEASE", autoStage);
        notice = new UpdateNotice(updateChecker, stager, runtimeConfigs, server.scheduler);
    }

    private void found(String... versions) {
        List<ModrinthVersion> listed = Arrays.stream(versions)
            .map(version -> new ModrinthVersion(version, "release", Instant.EPOCH, List.of("26.3"),
                List.of("paper", "spigot"), null))
            .toList();
        when(updateChecker.getUpdates()).thenReturn(Updates.of(listed, "RELEASE", "3.7.1", ServerPlatform.spigot("26.3")));
    }

    private List<String> lines() {
        return notice.lines().stream().map(Msg::plainText).toList();
    }

    @Test
    public void availableUpdateIsShownWithAClickableCommand() {
        found("3.8.1", "3.7.1");

        assertEquals(List.of("uSkyBlock 3.8.1 is available. /usb update stages it for the next restart."), lines());
        assertTrue(clickEvents(notice.lines().getFirst()).contains(ClickEvent.runCommand("/usb update")));
    }

    @Test
    public void stagedUpdateIsShownInstead() {
        found("3.8.1", "3.7.1");
        when(stager.staged()).thenReturn(new UpdateStager.StagedUpdate("3.8.1", false));

        assertEquals(List.of("uSkyBlock 3.8.1 is staged and takes effect at the next restart."), lines());
    }

    @Test
    public void autoStagedUpdateSaysSo() {
        found("3.8.1", "3.7.1");
        when(stager.staged()).thenReturn(new UpdateStager.StagedUpdate("3.8.1", true));

        assertEquals(List.of("uSkyBlock 3.8.1 was staged automatically and takes effect at the next restart."), lines());
    }

    @Test
    public void newerUpdateThanTheStagedOneIsShownToo() {
        found("3.8.2", "3.8.1", "3.7.1");
        when(stager.staged()).thenReturn(new UpdateStager.StagedUpdate("3.8.1", true));

        assertEquals(List.of("uSkyBlock 3.8.1 was staged automatically and takes effect at the next restart.",
            "uSkyBlock 3.8.2 is available. /usb update stages it for the next restart."), lines());
    }

    @Test
    public void newMajorVersionHasItsOwnLineWithItsRequirementAndTheUpgradeGuide() {
        found("4.0.0", "3.8.1", "3.7.1");

        assertEquals(List.of("uSkyBlock 3.8.1 is available. /usb update stages it for the next restart.",
            "uSkyBlock 4.0.0 is out (needs Minecraft 26.3). Read the upgrade guide first: " + GUIDE), lines());
        assertTrue(clickEvents(notice.lines().getLast()).contains(ClickEvent.openUrl(GUIDE)));
    }

    @Test
    public void stagedNewMajorVersionIsShownOnlyAsStaged() {
        found("4.0.0", "3.7.1");
        when(stager.staged()).thenReturn(new UpdateStager.StagedUpdate("4.0.0", false));

        assertEquals(List.of("uSkyBlock 4.0.0 is staged and takes effect at the next restart."), lines());
    }

    @Test
    public void nothingWhenUpToDate() {
        found("3.7.1", "3.7.0");

        assertEquals(List.of(), lines());
    }

    @Test
    public void nothingBeforeAnyCheck() {
        assertEquals(List.of(), lines());
    }

    @Test
    public void nothingWhenUpdateChecksAreOff() {
        found("4.0.0", "3.8.1");
        when(stager.staged()).thenReturn(new UpdateStager.StagedUpdate("3.8.1", true));
        notice(false);

        assertEquals(List.of(), lines());
    }

    @Test
    public void shownAfterJoiningWithThePermission() throws InterruptedException {
        found("3.8.1", "3.7.1");
        Player admin = mock(Player.class);
        when(admin.hasPermission("usb.admin.update")).thenReturn(true);
        when(admin.isOnline()).thenReturn(true);
        List<String> sent = new ArrayList<>();
        Msg.configure((sender, message) -> sent.add(Msg.plainText(message)));
        try {
            notice.onJoin(new PlayerJoinEvent(admin, "joined"));
            verify(server.scheduler).sync(any(Runnable.class), any(Duration.class));
            server.runNextTask();
        } finally {
            Msg.configure(null);
        }

        assertEquals(List.of("uSkyBlock 3.8.1 is available. /usb update stages it for the next restart."), sent);
    }

    @Test
    public void notShownWithoutThePermission() {
        found("3.8.1", "3.7.1");
        Player player = mock(Player.class);

        notice.onJoin(new PlayerJoinEvent(player, "joined"));

        verify(server.scheduler, never()).sync(any(Runnable.class), any(Duration.class));
    }

    @Test
    public void withAutoStageAnUpdateWaitingOutTheDelayShowsNothing() {
        notice(true, true);
        found("3.8.1", "3.7.1");

        assertEquals(List.of(), lines());
    }

    @Test
    public void withAutoStageOnlyTheStagedLineShows() {
        notice(true, true);
        found("3.8.2", "3.8.1", "3.7.1");
        when(stager.staged()).thenReturn(new UpdateStager.StagedUpdate("3.8.1", true));

        assertEquals(List.of("uSkyBlock 3.8.1 was staged automatically and takes effect at the next restart."), lines());
    }

    @Test
    public void withAutoStageANewMajorVersionShowsOnlyItsLine() {
        notice(true, true);
        found("4.0.0", "3.8.1", "3.7.1");

        assertEquals(List.of("uSkyBlock 4.0.0 is out (needs Minecraft 26.3). Read the upgrade guide first: " + GUIDE),
            lines());
    }

    private static List<ClickEvent> clickEvents(Component component) {
        List<ClickEvent> events = new ArrayList<>();
        if (component.clickEvent() != null) {
            events.add(component.clickEvent());
        }
        component.children().forEach(child -> events.addAll(clickEvents(child)));
        return events;
    }
}
