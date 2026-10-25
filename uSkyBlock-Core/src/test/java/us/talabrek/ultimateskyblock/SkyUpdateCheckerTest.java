package us.talabrek.ultimateskyblock;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.api.plugin.UpdateChecker;
import us.talabrek.ultimateskyblock.bootstrap.PluginLog;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static us.talabrek.ultimateskyblock.SkyUpdateChecker.parseLatestVersion;
import static us.talabrek.ultimateskyblock.SkyUpdateChecker.updateUri;

public class SkyUpdateCheckerTest {

    // Trimmed GET /repos/uskyblock/uSkyBlock/releases/latest response.
    private static final String GITHUB_LATEST_RELEASE = """
        {
          "html_url": "https://github.com/uskyblock/uSkyBlock/releases/tag/v3.7.1",
          "tag_name": "v3.7.1",
          "target_commitish": "master",
          "name": "3.7.1 - Maintenance and bug fixes",
          "draft": false,
          "prerelease": false,
          "published_at": "2026-10-25T16:06:41Z",
          "assets": []
        }
        """;

    private static final String MANUAL_CHECK_HINT = "Failed to check for new uSkyBlock versions. "
        + "Please check for updates manually at https://uskyblock.github.io/";

    private final SkyUpdateChecker checker = new SkyUpdateChecker(
        mock(uSkyBlock.class), mock(RuntimeConfigs.class), Logger.getLogger("SkyUpdateCheckerTest"));

    private final List<String> logged = new ArrayList<>();
    private final SkyUpdateChecker loggingChecker = checkerOn("3.7.1", logged);

    private static SkyUpdateChecker checkerOn(String currentVersion, List<String> logged) {
        uSkyBlock plugin = mock(uSkyBlock.class);
        when(plugin.getDescription()).thenReturn(new PluginDescriptionFile("uSkyBlock", currentVersion, "main"));
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                logged.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        return new SkyUpdateChecker(plugin, mock(RuntimeConfigs.class), logger);
    }

    @Test
    public void readsTagNameWithLeadingV() {
        assertEquals("3.7.1", parseLatestVersion(200, "{\"tag_name\": \"v3.7.1\"}"));
    }

    @Test
    public void readsTagNameWithoutLeadingV() {
        assertEquals("3.7.1", parseLatestVersion(200, "{\"tag_name\": \"3.7.1\"}"));
    }

    @Test
    public void readsLatestGitHubRelease() {
        assertEquals("3.7.1", parseLatestVersion(200, GITHUB_LATEST_RELEASE));
    }

    @Test
    public void readsStagingFeedVersion() {
        String staging = "{\"version\": \"3.8.0-SNAPSHOT\", \"build\": \"493\"}";
        assertEquals("3.8.0-SNAPSHOT", parseLatestVersion(200, staging));
    }

    @Test
    public void nonSuccessStatusGivesNoVersion() {
        String rateLimited = "{\"message\": \"API rate limit exceeded for 203.0.113.7.\"}";
        assertNull(parseLatestVersion(403, rateLimited));
        assertNull(parseLatestVersion(429, rateLimited));
        assertNull(parseLatestVersion(404, "{\"message\": \"Not Found\"}"));
        assertNull(parseLatestVersion(500, GITHUB_LATEST_RELEASE));
    }

    @Test
    public void badBodyGivesNoVersion() {
        assertNull(parseLatestVersion(200, "{\"tag_name\": "));
        assertNull(parseLatestVersion(200, "<html><body>Bad gateway</body></html>"));
        assertNull(parseLatestVersion(200, "[]"));
        assertNull(parseLatestVersion(200, ""));
        assertNull(parseLatestVersion(200, null));
        assertNull(parseLatestVersion(200, "{\"name\": \"3.7.1\"}"));
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
        assertTrue(checker.isNewerVersion("3.7.0", parseLatestVersion(200, GITHUB_LATEST_RELEASE)));
        assertFalse(checker.isNewerVersion("3.7.1", parseLatestVersion(200, GITHUB_LATEST_RELEASE)));
    }

    @Test
    public void releaseBranchAsksGitHub() {
        URI github = URI.create("https://api.github.com/repos/uskyblock/uSkyBlock/releases/latest");
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

        assertEquals(List.of(MANUAL_CHECK_HINT), logged);
        assertTrue(loggingChecker.lastCheckFailed());
    }

    @Test
    public void repeatedFailureLogsHintOnlyOnce() {
        loggingChecker.handleCheckResult(null);
        loggingChecker.handleCheckResult(null);
        loggingChecker.handleCheckResult(null);

        assertEquals(List.of(MANUAL_CHECK_HINT), logged);
    }

    @Test
    public void failureAfterSuccessfulCheckLogsHintAgain() {
        loggingChecker.handleCheckResult(null);
        loggingChecker.handleCheckResult("3.7.1");
        assertFalse(loggingChecker.lastCheckFailed());
        loggingChecker.handleCheckResult(null);

        assertEquals(List.of(MANUAL_CHECK_HINT, MANUAL_CHECK_HINT), logged);
    }

    @Test
    public void successfulCheckDoesNotLogHint() {
        loggingChecker.handleCheckResult("3.7.1");

        assertEquals(List.of(), logged);
        assertFalse(loggingChecker.lastCheckFailed());
    }

    @Test
    public void lastCheckIsNotFailedBeforeAnyCheck() {
        assertFalse(loggingChecker.lastCheckFailed());
    }

    @Test
    public void newerVersionIsStillAnnounced() {
        loggingChecker.handleCheckResult("3.8.0");

        assertEquals(List.of(
            "There is a new version of uSkyBlock available: 3.8.0",
            "Visit https://uskyblock.github.io/ to download."), logged);
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
