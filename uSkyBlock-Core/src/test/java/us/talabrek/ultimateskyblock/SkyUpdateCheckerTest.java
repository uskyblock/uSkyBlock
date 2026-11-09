package us.talabrek.ultimateskyblock;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import us.talabrek.ultimateskyblock.api.plugin.UpdateChecker;
import us.talabrek.ultimateskyblock.bootstrap.PluginLog;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.update.ModrinthClient;
import us.talabrek.ultimateskyblock.update.ModrinthStandIn;
import us.talabrek.ultimateskyblock.update.ModrinthVersion;
import us.talabrek.ultimateskyblock.update.ServerPlatform;
import us.talabrek.ultimateskyblock.update.UpdateServer;
import us.talabrek.ultimateskyblock.update.UpdateStager;
import us.talabrek.ultimateskyblock.update.Updates;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static us.talabrek.ultimateskyblock.SkyUpdateChecker.parseLatestVersion;

public class SkyUpdateCheckerTest {

    private static final Instant PUBLISHED = Instant.parse("2026-10-25T16:07:41Z");
    private static final List<String> PAPER_SPIGOT = List.of("paper", "spigot");
    private static final List<String> MC_26_3 = List.of("26.2", "26.3");

    private static final String MANUAL_CHECK_HINT = "Failed to check for new uSkyBlock versions. "
        + "Please check for updates manually at https://modrinth.com/plugin/uskyblock";

    @TempDir
    Path plugins;

    private final ModrinthStandIn modrinth;
    private final List<LogRecord> records = new ArrayList<>();
    private final UpdateStager stager = mock(UpdateStager.class);
    private RuntimeConfigs runtimeConfigs;

    public SkyUpdateCheckerTest() throws IOException {
        modrinth = new ModrinthStandIn();
        config("RELEASE", false);
    }

    @AfterEach
    public void stopModrinth() {
        modrinth.close();
    }

    private void config(String branch, boolean autoStage) {
        runtimeConfigs = UpdateServer.config(true, branch, autoStage);
    }

    private SkyUpdateChecker checker(ModrinthClient client) {
        UpdateServer server = new UpdateServer(plugins, "3.7.1");
        return new SkyUpdateChecker(server.plugin, runtimeConfigs, loggerInto(records), client, stager,
            ServerPlatform.spigot("26.3"));
    }

    private SkyUpdateChecker checker() {
        return checker(modrinth.client());
    }

    private static Logger loggerInto(List<LogRecord> records) {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        return logger;
    }

    private List<String> logged() {
        return records.stream().map(LogRecord::getMessage).toList();
    }

    private static Updates updates(String currentVersion, ModrinthVersion... versions) {
        return Updates.of(List.of(versions), "RELEASE", currentVersion, ServerPlatform.spigot("26.3"));
    }

    private static ModrinthVersion version(String version, List<String> gameVersions) {
        return new ModrinthVersion(version, "release", PUBLISHED, gameVersions, PAPER_SPIGOT, null);
    }

    @Test
    public void newerCompatibleVersionIsAnUpdate() {
        modrinth.version("3.7.1", "release", PUBLISHED, MC_26_3, PAPER_SPIGOT)
            .version("3.8.0", "release", PUBLISHED, MC_26_3, PAPER_SPIGOT);
        SkyUpdateChecker checker = checker();

        checker.checkForUpdates().join();

        assertTrue(checker.isUpdateAvailable());
        assertEquals("3.8.0", checker.getLatestVersion());
        assertNull(checker.getNewMajorVersion());
        assertEquals(List.of(
            "There is a new version of uSkyBlock available: 3.8.0",
            "Run /usb update to stage it for the next restart, or download it from "
                + "https://modrinth.com/plugin/uskyblock/version/3.8.0"), logged());
    }

    @Test
    public void newerVersionForAnotherMinecraftVersionOrLoaderIsNoUpdate() {
        modrinth.version("3.7.1", "release", PUBLISHED, MC_26_3, PAPER_SPIGOT)
            .version("3.8.1", "release", PUBLISHED, List.of("26.4"), PAPER_SPIGOT)
            .version("3.8.0", "release", PUBLISHED, MC_26_3, List.of("paper"));
        SkyUpdateChecker checker = checker();

        checker.checkForUpdates().join();

        assertFalse(checker.isUpdateAvailable());
        assertEquals("3.7.1", checker.getLatestVersion());
        assertEquals(List.of(), logged());
    }

    @Test
    public void newMajorVersionIsAnnouncedWithItsRequirementAlsoWhenIncompatible() {
        modrinth.version("3.7.1", "release", PUBLISHED, MC_26_3, PAPER_SPIGOT)
            .version("4.0.0", "release", PUBLISHED, List.of("26.4"), List.of("paper"));
        SkyUpdateChecker checker = checker();

        checker.checkForUpdates().join();

        assertFalse(checker.isUpdateAvailable());
        assertEquals("4.0.0", checker.getNewMajorVersion().version());
        assertEquals(List.of(
            "There is a new major version of uSkyBlock available: 4.0.0 (needs Minecraft 26.4)",
            "Read the upgrade guide before upgrading: https://uskyblock.github.io/",
            "Release notes: https://modrinth.com/plugin/uskyblock/version/4.0.0"), logged());
    }

    @Test
    public void betaAndAlphaAreIgnoredOnTheReleaseChannel() {
        modrinth.version("3.7.1", "release", PUBLISHED, MC_26_3, PAPER_SPIGOT)
            .version("3.8.0-beta", "beta", PUBLISHED, MC_26_3, PAPER_SPIGOT)
            .version("4.0.0-alpha", "alpha", PUBLISHED, MC_26_3, PAPER_SPIGOT);
        SkyUpdateChecker checker = checker();

        checker.checkForUpdates().join();

        assertFalse(checker.isUpdateAvailable());
        assertNull(checker.getNewMajorVersion());
        assertEquals(List.of(), logged());
    }

    @Test
    public void stagingChannelOffersBetaAndAlpha() {
        config("STAGING", false);
        modrinth.version("3.7.1", "release", PUBLISHED, MC_26_3, PAPER_SPIGOT)
            .version("3.8.0-beta", "beta", PUBLISHED, MC_26_3, PAPER_SPIGOT);
        SkyUpdateChecker checker = checker();

        checker.checkForUpdates().join();

        assertEquals("3.8.0-beta", checker.getLatestVersion());
    }

    @Test
    public void unreachableModrinthLogsOnlyTheHintOnce() {
        SkyUpdateChecker checker = checker();
        modrinth.close();

        checker.checkForUpdates().join();
        checker.checkForUpdates().join();

        assertEquals(List.of(MANUAL_CHECK_HINT), logged());
        assertEquals(Level.INFO, records.getFirst().getLevel());
        assertNull(records.getFirst().getThrown());
        assertTrue(checker.lastCheckFailed());
    }

    @Test
    public void malformedAnswerIsAFailedCheck() {
        modrinth.answer(200, "<html><body>Bad gateway</body></html>");
        SkyUpdateChecker checker = checker();

        checker.checkForUpdates().join();

        assertEquals(List.of(MANUAL_CHECK_HINT), logged());
        assertTrue(checker.lastCheckFailed());
    }

    @Test
    public void errorStatusIsAFailedCheck() {
        modrinth.rateLimited("60");
        SkyUpdateChecker checker = checker();

        checker.checkForUpdates().join();

        assertEquals(List.of(MANUAL_CHECK_HINT), logged());
    }

    @Test
    public void unexpectedFailureLogsStackTraceOnce() throws IOException {
        IllegalStateException bug = new IllegalStateException("bug");
        ModrinthClient client = mock(ModrinthClient.class);
        when(client.body(any())).thenThrow(bug);
        SkyUpdateChecker checker = checker(client);

        checker.checkForUpdates().join();
        checker.checkForUpdates().join();

        assertEquals(List.of("Exception while trying to fetch latest plugin version.", MANUAL_CHECK_HINT), logged());
        assertEquals(Level.SEVERE, records.getFirst().getLevel());
        assertSame(bug, records.getFirst().getThrown());
    }

    @Test
    public void failureAfterSuccessfulCheckLogsHintAgainAndForgetsTheVersions() {
        SkyUpdateChecker checker = checker();
        checker.handleCheckResult(null);
        checker.handleCheckResult(updates("3.7.1", version("3.7.1", MC_26_3), version("4.0.0", MC_26_3)));
        assertFalse(checker.lastCheckFailed());
        records.clear();

        checker.handleCheckResult(null);

        assertEquals(List.of(MANUAL_CHECK_HINT), logged());
        assertNull(checker.getNewMajorVersion());
        assertNull(checker.getLatestVersion());
        assertNull(checker.getUpdates());
    }

    @Test
    public void lastCheckIsNotFailedBeforeAnyCheck() {
        assertFalse(checker().lastCheckFailed());
    }

    @Test
    public void updateAndNewMajorVersionAreBothAnnounced() {
        SkyUpdateChecker checker = checker();

        checker.handleCheckResult(updates("3.7.1", version("3.7.2", MC_26_3), version("4.0.0", List.of("26.3"))));

        assertEquals(List.of(
            "There is a new version of uSkyBlock available: 3.7.2",
            "Run /usb update to stage it for the next restart, or download it from "
                + "https://modrinth.com/plugin/uskyblock/version/3.7.2",
            "There is a new major version of uSkyBlock available: 4.0.0 (needs Minecraft 26.3)",
            "Read the upgrade guide before upgrading: https://uskyblock.github.io/",
            "Release notes: https://modrinth.com/plugin/uskyblock/version/4.0.0"), logged());
    }

    @Test
    public void checkAutoStagesOnlyWhenAutoStageIsOn() {
        modrinth.version("3.8.0", "release", PUBLISHED, MC_26_3, PAPER_SPIGOT);
        when(stager.autoStage(any(), any())).thenReturn(CompletableFuture.completedFuture(null));

        checker().checkForUpdates().join();
        verify(stager, never()).autoStage(any(), any());

        config("RELEASE", true);
        checker().checkForUpdates().join();
        verify(stager).autoStage(any(), any());
    }

    @Test
    public void failedCheckDoesNotAutoStage() {
        config("RELEASE", true);
        SkyUpdateChecker checker = checker();
        modrinth.close();

        checker.checkForUpdates().join();

        verify(stager, never()).autoStage(any(), any());
    }

    @Test
    public void latestVersionApiReportsOwnMajorVersion() {
        modrinth.version("3.7.2", "release", PUBLISHED, MC_26_3, PAPER_SPIGOT)
            .version("4.0.0", "release", PUBLISHED, MC_26_3, PAPER_SPIGOT);
        ModrinthClient client = modrinth.client();

        assertEquals("3.7.2", checker(client).fetchLatestVersion(client.versionsUri()).join());
    }

    @Test
    public void latestVersionApiReadsAVersionFeed() {
        ServerPlatform spigot = ServerPlatform.spigot("26.3");

        assertEquals("3.8.0-SNAPSHOT",
            parseLatestVersion("{\"version\": \"3.8.0-SNAPSHOT\", \"build\": \"493\"}", "STAGING", "3.7.1", spigot));
        assertNull(parseLatestVersion("{\"name\": \"3.7.1\"}", "RELEASE", "3.7.1", spigot));
        assertNull(parseLatestVersion("<html></html>", "RELEASE", "3.7.1", spigot));
    }

    @Test
    public void isNewerVersionComparesAsBefore() {
        SkyUpdateChecker checker = checker();

        assertTrue(checker.isNewerVersion("3.7.0", "3.7.1"));
        assertTrue(checker.isNewerVersion("3.7.1", "3.8.0"));
        assertTrue(checker.isNewerVersion("3.8.0-SNAPSHOT", "3.8.0"));
        assertFalse(checker.isNewerVersion("3.7.1", "3.7.1"));
        assertFalse(checker.isNewerVersion("3.7.1", "3.7.0"));
        assertFalse(checker.isNewerVersion("3.8.0-SNAPSHOT", "3.7.1"));
    }

    @Test
    public void everyInjectionSharesTheCheckedInstance() {
        // The scheduled check, /usb version and /usb update must see the same check result.
        uSkyBlock plugin = mock(uSkyBlock.class);
        Injector injector = Guice.createInjector(binder -> {
            // A provider, so Guice doesn't inject the plugin's own fields.
            binder.bind(uSkyBlock.class).toProvider(() -> plugin);
            binder.bind(RuntimeConfigs.class).toInstance(mock(RuntimeConfigs.class));
            binder.bind(Logger.class).annotatedWith(PluginLog.class).toInstance(Logger.getLogger("SkyUpdateCheckerTest"));
            binder.bind(ModrinthClient.class).toInstance(mock(ModrinthClient.class));
            binder.bind(UpdateStager.class).toInstance(stager);
            binder.bind(Clock.class).toInstance(Clock.systemUTC());
            binder.bind(ServerPlatform.class).toInstance(ServerPlatform.spigot("26.3"));
            binder.bind(UpdateChecker.class).to(SkyUpdateChecker.class);
        });

        assertSame(injector.getInstance(SkyUpdateChecker.class), injector.getInstance(UpdateChecker.class));
    }
}
