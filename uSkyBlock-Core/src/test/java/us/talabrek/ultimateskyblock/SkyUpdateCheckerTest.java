package us.talabrek.ultimateskyblock;

import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static us.talabrek.ultimateskyblock.SkyUpdateChecker.parseLatestVersion;

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

    private final SkyUpdateChecker checker = new SkyUpdateChecker(
        mock(uSkyBlock.class), mock(RuntimeConfigs.class), Logger.getLogger("SkyUpdateCheckerTest"));

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
}
