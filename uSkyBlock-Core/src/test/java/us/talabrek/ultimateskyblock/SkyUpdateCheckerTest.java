package us.talabrek.ultimateskyblock;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.SkyUpdateChecker.CheckResult;
import us.talabrek.ultimateskyblock.SkyUpdateChecker.Release;
import us.talabrek.ultimateskyblock.api.plugin.UpdateChecker;
import us.talabrek.ultimateskyblock.bootstrap.PluginLog;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;

import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static us.talabrek.ultimateskyblock.SkyUpdateChecker.parseCheckResult;
import static us.talabrek.ultimateskyblock.SkyUpdateChecker.updateUri;

public class SkyUpdateCheckerTest {

    // Trimmed GET /repos/uskyblock/uSkyBlock/releases?per_page=100 response.
    private static final String GITHUB_RELEASES = """
        [
          {
            "html_url": "https://github.com/uskyblock/uSkyBlock/releases/tag/v3.7.1",
            "tag_name": "v3.7.1",
            "target_commitish": "master",
            "name": "3.7.1 - Maintenance and bug fixes",
            "draft": false,
            "prerelease": false,
            "published_at": "2026-10-25T16:06:41Z",
            "assets": []
          },
          {
            "html_url": "https://github.com/uskyblock/uSkyBlock/releases/tag/v3.7.0",
            "tag_name": "v3.7.0",
            "target_commitish": "master",
            "name": "3.7.0 - Maintenance and bug fixes",
            "draft": false,
            "prerelease": false,
            "published_at": "2026-10-25T03:11:04Z",
            "assets": []
          }
        ]
        """;

    private static final Release RELEASE_4_0_0 =
        new Release("4.0.0", "https://github.com/uskyblock/uSkyBlock/releases/tag/v4.0.0");

    private static final String MANUAL_CHECK_HINT = "Failed to check for new uSkyBlock versions. "
        + "Please check for updates manually at https://uskyblock.github.io/";

    private final SkyUpdateChecker checker = new SkyUpdateChecker(
        mock(uSkyBlock.class), mock(RuntimeConfigs.class), Logger.getLogger("SkyUpdateCheckerTest"));

    private final List<LogRecord> records = new ArrayList<>();
    private final SkyUpdateChecker loggingChecker = new SkyUpdateChecker(
        pluginOn("3.7.1"), mock(RuntimeConfigs.class), loggerInto(records));

    private static uSkyBlock pluginOn(String currentVersion) {
        uSkyBlock plugin = mock(uSkyBlock.class);
        when(plugin.getDescription()).thenReturn(new PluginDescriptionFile("uSkyBlock", currentVersion, "main"));
        when(plugin.isEnabled()).thenReturn(true);
        return plugin;
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

    private static String release(String tag) {
        return release(tag, false, false);
    }

    private static String release(String tag, boolean draft, boolean prerelease) {
        return """
            {"html_url": "https://github.com/uskyblock/uSkyBlock/releases/tag/%s", "tag_name": "%s",
             "draft": %s, "prerelease": %s}
            """.formatted(tag, tag, draft, prerelease);
    }

    private static String releases(String... releases) {
        return "[" + String.join(",", releases) + "]";
    }

    private interface Request {
        CheckResult send() throws IOException;
    }

    private SkyUpdateChecker checkerRequesting(Request request) {
        return new SkyUpdateChecker(pluginOn("3.7.1"), mock(RuntimeConfigs.class), loggerInto(records)) {
            @Override
            CheckResult requestCheckResult(URI uri) throws IOException {
                return request.send();
            }
        };
    }

    // What checkForUpdates does, without the scheduler and the runtime config.
    private static CheckResult check(SkyUpdateChecker checker) {
        CheckResult result = checker.fetchCheckResult(UpdateChecker.URL_STAGING).join();
        checker.handleCheckResult(result);
        return result;
    }

    @Test
    public void readsTagNameWithLeadingV() {
        assertEquals(new CheckResult("3.7.1", null), parseCheckResult(200, releases(release("v3.7.1")), "3.7.0"));
    }

    @Test
    public void readsTagNameWithoutLeadingV() {
        assertEquals(new CheckResult("3.7.1", null), parseCheckResult(200, releases(release("3.7.1")), "3.7.0"));
    }

    @Test
    public void readsGitHubReleases() {
        assertEquals(new CheckResult("3.7.1", null), parseCheckResult(200, GITHUB_RELEASES, "3.7.0"));
    }

    @Test
    public void latestReleaseOnOwnMajorVersionIsPickedWhenNewerMajorExists() {
        String body = releases(release("v4.0.0"), release("v3.7.1"), release("v3.7.0"));

        CheckResult result = parseCheckResult(200, body, "3.8.0-SNAPSHOT");

        assertEquals(new CheckResult("3.7.1", RELEASE_4_0_0), result);
    }

    @Test
    public void releasesArePickedByVersionNotByListOrder() {
        String body = releases(release("v3.7.0"), release("v4.1.0"), release("v3.10.0"), release("v4.0.0"),
            release("v3.9.2"));

        CheckResult result = parseCheckResult(200, body, "3.7.0");

        assertEquals(new CheckResult("3.10.0",
            new Release("4.1.0", "https://github.com/uskyblock/uSkyBlock/releases/tag/v4.1.0")), result);
    }

    @Test
    public void olderMajorVersionIsNotReported() {
        CheckResult result = parseCheckResult(200, GITHUB_RELEASES, "4.0.0-SNAPSHOT");

        assertEquals(new CheckResult(null, null), result);
    }

    @Test
    public void snapshotOfNewMajorVersionGetsItsReleaseAsRegularUpdate() {
        String body = releases(release("v4.0.0"), release("v3.7.1"));

        assertEquals(new CheckResult("4.0.0", null), parseCheckResult(200, body, "4.0.0-SNAPSHOT"));
    }

    @Test
    public void draftsAndPrereleasesAreSkipped() {
        String body = releases(release("v5.0.0", true, false), release("v4.0.0", false, true),
            release("v3.7.3", true, false), release("v3.7.2", false, true), release("v3.7.1"));

        assertEquals(new CheckResult("3.7.1", null), parseCheckResult(200, body, "3.7.0"));
    }

    @Test
    public void entriesWithoutVersionTagAreSkipped() {
        String body = releases(release("nightly"), release("v4"), release("build-493"), release("v"),
            "{\"tag_name\": null, \"html_url\": \"https://github.com/\"}", "{\"html_url\": \"https://github.com/\"}",
            "{\"tag_name\": \"v4.0.0\"}", "null", "\"v4.0.0\"", release("v3.7.1"));

        assertEquals(new CheckResult("3.7.1", null), parseCheckResult(200, body, "3.7.0"));
    }

    @Test
    public void emptyReleaseListIsNoFailure() {
        assertEquals(new CheckResult(null, null), parseCheckResult(200, "[]", "3.7.1"));
    }

    @Test
    public void readsStagingFeedVersion() {
        String staging = "{\"version\": \"3.8.0-SNAPSHOT\", \"build\": \"493\"}";
        assertEquals(new CheckResult("3.8.0-SNAPSHOT", null), parseCheckResult(200, staging, "3.7.1"));
    }

    @Test
    public void stagingFeedVersionIsReportedWhateverItsMajorVersion() {
        String staging = "{\"version\": \"4.0.0-SNAPSHOT\", \"build\": \"510\"}";
        assertEquals(new CheckResult("4.0.0-SNAPSHOT", null), parseCheckResult(200, staging, "3.8.0-SNAPSHOT"));
    }

    @Test
    public void nonSuccessStatusGivesNoVersion() {
        String rateLimited = "{\"message\": \"API rate limit exceeded for 203.0.113.7.\"}";
        assertNull(parseCheckResult(403, rateLimited, "3.7.1"));
        assertNull(parseCheckResult(429, rateLimited, "3.7.1"));
        assertNull(parseCheckResult(404, "{\"message\": \"Not Found\"}", "3.7.1"));
        assertNull(parseCheckResult(500, GITHUB_RELEASES, "3.7.1"));
    }

    @Test
    public void badBodyGivesNoVersion() {
        assertNull(parseCheckResult(200, "[{\"tag_name\": ", "3.7.1"));
        assertNull(parseCheckResult(200, "<html><body>Bad gateway</body></html>", "3.7.1"));
        assertNull(parseCheckResult(200, "", "3.7.1"));
        assertNull(parseCheckResult(200, null, "3.7.1"));
        assertNull(parseCheckResult(200, "{\"name\": \"3.7.1\"}", "3.7.1"));
    }

    @Test
    public void isNewerVersionComparesAsBefore() {
        assertTrue(checker.isNewerVersion("3.7.0", "3.7.1"));
        assertTrue(checker.isNewerVersion("3.7.1", "3.8.0"));
        assertTrue(checker.isNewerVersion("3.8.0-SNAPSHOT", "3.8.0"));
        assertFalse(checker.isNewerVersion("3.7.1", "3.7.1"));
        assertFalse(checker.isNewerVersion("3.7.1", "3.7.0"));
        assertFalse(checker.isNewerVersion("3.8.0-SNAPSHOT", "3.7.1"));
    }

    @Test
    public void latestTagIsComparedWithoutLeadingV() {
        String latest = parseCheckResult(200, GITHUB_RELEASES, "3.7.0").latestVersion();
        assertTrue(checker.isNewerVersion("3.7.0", latest));
        assertFalse(checker.isNewerVersion("3.7.1", latest));
    }

    @Test
    public void releaseBranchAsksGitHub() {
        URI github = URI.create("https://api.github.com/repos/uskyblock/uSkyBlock/releases?per_page=100");
        assertEquals(github, updateUri("RELEASE"));
    }

    @Test
    public void stagingBranchAsksStagingFeed() {
        assertEquals(UpdateChecker.URL_STAGING, updateUri("STAGING"));
        assertEquals(UpdateChecker.URL_STAGING, updateUri("staging"));
    }

    @Test
    public void failedCheckLogsManualCheckHint() {
        loggingChecker.handleCheckResult(null);

        assertEquals(List.of(MANUAL_CHECK_HINT), logged());
        assertTrue(loggingChecker.lastCheckFailed());
    }

    @Test
    public void repeatedFailureLogsHintOnlyOnce() {
        loggingChecker.handleCheckResult(null);
        loggingChecker.handleCheckResult(null);
        loggingChecker.handleCheckResult(null);

        assertEquals(List.of(MANUAL_CHECK_HINT), logged());
    }

    @Test
    public void failureAfterSuccessfulCheckLogsHintAgain() {
        loggingChecker.handleCheckResult(null);
        loggingChecker.handleCheckResult(new CheckResult("3.7.1", null));
        assertFalse(loggingChecker.lastCheckFailed());
        loggingChecker.handleCheckResult(null);

        assertEquals(List.of(MANUAL_CHECK_HINT, MANUAL_CHECK_HINT), logged());
    }

    @Test
    public void successfulCheckDoesNotLogHint() {
        loggingChecker.handleCheckResult(new CheckResult("3.7.1", null));

        assertEquals(List.of(), logged());
        assertFalse(loggingChecker.lastCheckFailed());
    }

    @Test
    public void lastCheckIsNotFailedBeforeAnyCheck() {
        assertFalse(loggingChecker.lastCheckFailed());
    }

    @Test
    public void newerVersionIsStillAnnounced() {
        loggingChecker.handleCheckResult(new CheckResult("3.8.0", null));

        assertEquals(List.of(
            "There is a new version of uSkyBlock available: 3.8.0",
            "Visit https://uskyblock.github.io/ to download."), logged());
    }

    @Test
    public void newMajorVersionIsAnnouncedWhenOwnMajorVersionIsCurrent() {
        loggingChecker.handleCheckResult(new CheckResult("3.7.1", RELEASE_4_0_0));

        assertEquals(List.of(
            "There is a new major version of uSkyBlock available: 4.0.0",
            "Read its release notes before upgrading: https://github.com/uskyblock/uSkyBlock/releases/tag/v4.0.0"),
            logged());
        assertFalse(loggingChecker.isUpdateAvailable());
        assertEquals("3.7.1", loggingChecker.getLatestVersion());
        assertEquals(RELEASE_4_0_0, loggingChecker.getNewMajorRelease());
    }

    @Test
    public void updateAndNewMajorVersionAreBothAnnounced() {
        loggingChecker.handleCheckResult(new CheckResult("3.7.2", RELEASE_4_0_0));

        assertEquals(List.of(
            "There is a new version of uSkyBlock available: 3.7.2",
            "Visit https://uskyblock.github.io/ to download.",
            "There is a new major version of uSkyBlock available: 4.0.0",
            "Read its release notes before upgrading: https://github.com/uskyblock/uSkyBlock/releases/tag/v4.0.0"),
            logged());
        assertEquals("3.7.2", loggingChecker.getLatestVersion());
    }

    @Test
    public void failedCheckForgetsNewMajorVersion() {
        loggingChecker.handleCheckResult(new CheckResult("3.7.1", RELEASE_4_0_0));
        loggingChecker.handleCheckResult(null);

        assertNull(loggingChecker.getNewMajorRelease());
        assertNull(loggingChecker.getLatestVersion());
        assertTrue(loggingChecker.lastCheckFailed());
    }

    @Test
    public void latestVersionApiReportsOwnMajorVersion() {
        SkyUpdateChecker checker = checkerRequesting(() -> new CheckResult("3.7.1", RELEASE_4_0_0));

        assertEquals("3.7.1", checker.fetchLatestVersion(UpdateChecker.URL_STAGING).join());
    }

    @Test
    public void unreachableUpdateSourceLogsOnlyTheHintOnce() {
        SkyUpdateChecker checker = checkerRequesting(() -> {
            throw new UnknownHostException("api.github.com");
        });

        assertNull(check(checker));
        assertEquals(List.of(MANUAL_CHECK_HINT), logged());
        assertEquals(Level.INFO, records.getFirst().getLevel());
        assertNull(records.getFirst().getThrown());

        assertNull(check(checker));
        assertEquals(List.of(MANUAL_CHECK_HINT), logged());
    }

    @Test
    public void unexpectedFailureLogsStackTraceOnce() {
        IllegalStateException bug = new IllegalStateException("bug");
        SkyUpdateChecker checker = checkerRequesting(() -> {
            throw bug;
        });

        assertNull(check(checker));
        assertEquals(List.of("Exception while trying to fetch latest plugin version.", MANUAL_CHECK_HINT), logged());
        assertEquals(Level.SEVERE, records.getFirst().getLevel());
        assertSame(bug, records.getFirst().getThrown());

        assertNull(check(checker));
        assertEquals(2, records.size());
    }

    @Test
    public void everyInjectionSharesTheCheckedInstance() {
        // The scheduled check and /usb version must see the same check result.
        uSkyBlock plugin = mock(uSkyBlock.class);
        Injector injector = Guice.createInjector(binder -> {
            // A provider, so Guice doesn't inject the plugin's own fields.
            binder.bind(uSkyBlock.class).toProvider(() -> plugin);
            binder.bind(RuntimeConfigs.class).toInstance(mock(RuntimeConfigs.class));
            binder.bind(Logger.class).annotatedWith(PluginLog.class).toInstance(Logger.getLogger("SkyUpdateCheckerTest"));
            binder.bind(UpdateChecker.class).to(SkyUpdateChecker.class);
        });

        assertSame(injector.getInstance(SkyUpdateChecker.class), injector.getInstance(UpdateChecker.class));
    }
}
