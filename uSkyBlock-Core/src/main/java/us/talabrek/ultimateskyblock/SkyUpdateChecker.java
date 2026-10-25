package us.talabrek.ultimateskyblock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.api.plugin.UpdateChecker;
import us.talabrek.ultimateskyblock.bootstrap.PluginLog;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;

import java.io.IOException;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class SkyUpdateChecker implements UpdateChecker {

    // Drafts and prereleases are skipped, so a withdrawn release can be hidden by turning it into either.
    private static final URI GITHUB_RELEASES =
        URI.create("https://api.github.com/repos/uskyblock/uSkyBlock/releases?per_page=100");

    // A version starts with major.minor, e.g. 3.7.1 or 3.8.0-SNAPSHOT.
    private static final Pattern VERSION = Pattern.compile("(\\d{1,9})\\.\\d+.*");

    private final Logger logger;
    private final RuntimeConfigs runtimeConfigs;
    private final uSkyBlock plugin;

    private String latestVersion;
    private volatile Release newMajorRelease;
    private volatile boolean lastCheckFailed;

    /**
     * A published uSkyBlock release.
     *
     * @param version The version, without the tag's leading "v".
     * @param url     The release page, with the release notes.
     */
    public record Release(@NotNull String version, @NotNull String url) {
    }

    /**
     * What an update check found.
     *
     * @param latestVersion   The latest release on the running major version, or the version feed's version, NULL if
     *                        there is none.
     * @param newMajorRelease The latest release, if it has a higher major version than the running one.
     */
    record CheckResult(@Nullable String latestVersion, @Nullable Release newMajorRelease) {
    }

    @Inject
    public SkyUpdateChecker(
        @NotNull uSkyBlock plugin,
        @NotNull RuntimeConfigs runtimeConfigs,
        @NotNull @PluginLog Logger logger
    ) {
        this.plugin = plugin;
        this.runtimeConfigs = runtimeConfigs;
        this.logger = logger;
    }

    /**
     * Triggers an update of the latest version info (the GitHub releases, or the staging feed on the STAGING
     * branch), and will log an INFO message if an update or a new major version is available or the check failed.
     */
    public void checkForUpdates() {
        URI uri = updateUri(runtimeConfigs.current().pluginUpdates().branch());

        fetchCheckResult(uri).thenAccept(this::handleCheckResult);
    }

    void handleCheckResult(@Nullable CheckResult result) {
        boolean failedBefore = lastCheckFailed;
        latestVersion = result != null ? result.latestVersion() : null;
        newMajorRelease = result != null ? result.newMajorRelease() : null;
        lastCheckFailed = result == null;
        // Only the first failure in a row is logged, so a lasting outage doesn't repeat it on every check.
        if (lastCheckFailed && !failedBefore) {
            logger.info("Failed to check for new uSkyBlock versions. "
                + "Please check for updates manually at https://uskyblock.github.io/");
        }

        if (isUpdateAvailable()) {
            logger.info("There is a new version of uSkyBlock available: " + getLatestVersion());
            logger.info("Visit https://uskyblock.github.io/ to download.");
        }
        if (newMajorRelease != null) {
            logger.info("There is a new major version of uSkyBlock available: " + newMajorRelease.version());
            logger.info("Read its release notes before upgrading: " + newMajorRelease.url());
        }
    }

    /**
     * @return True if the most recent update check got no version info, so admins have to check manually.
     */
    public boolean lastCheckFailed() {
        return lastCheckFailed;
    }

    /**
     * @return The latest release if it has a higher major version than the running one, NULL otherwise. It is
     * announced apart from regular updates, as upgrading to it may need more than replacing the jar.
     */
    public @Nullable Release getNewMajorRelease() {
        return newMajorRelease;
    }

    static @NotNull URI updateUri(@NotNull String branch) {
        return branch.equalsIgnoreCase("STAGING") ? URL_STAGING : GITHUB_RELEASES;
    }

    public boolean isUpdateAvailable() {
        if (latestVersion != null) {
            return isNewerVersion(getCurrentVersion(), getLatestVersion());
        }
        return false;
    }

    public @Nullable String getLatestVersion() {
        return latestVersion;
    }

    public @NotNull String getCurrentVersion() {
        return plugin.getDescription().getVersion();
    }

    public CompletableFuture<String> fetchLatestVersion(URI uri) {
        return fetchCheckResult(uri).thenApply(result -> result != null ? result.latestVersion() : null);
    }

    CompletableFuture<CheckResult> fetchCheckResult(URI uri) {
        CompletableFuture<CheckResult> future = new CompletableFuture<>();
        future.completeAsync(() -> {
            try {
                return requestCheckResult(uri);
            } catch (IOException ex) {
                // The update source can't be reached (no network, DNS, timeout, TLS). That is expected on some
                // hosts and not a plugin bug, so nothing is logged here; checkForUpdates logs the manual-check hint.
            } catch (Exception ex) {
                // This runs asynchronously and can still be in flight when the server shuts down or
                // restarts, tearing down the plugin classloader mid-request. Such a failure is expected
                // and not actionable, so only surface it while the plugin is actually enabled. Like the
                // manual-check hint, it is only logged on the first failed check in a row.
                if (plugin.isEnabled() && !lastCheckFailed) {
                    logger.log(Level.SEVERE, "Exception while trying to fetch latest plugin version.", ex);
                }
            }

            return null;
        });

        return future;
    }

    @Nullable CheckResult requestCheckResult(@NotNull URI uri) throws IOException {
        String userAgent = "uSkyBlock-Plugin/v" + getCurrentVersion() + " (www.uskyblock.ovh)";
        Timeout timeout = Timeout.ofSeconds(10);
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
            .setConnectTimeout(timeout)
            .setSocketTimeout(timeout)
            .build();
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(timeout)
            .build();
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
            .setDefaultConnectionConfig(connectionConfig)
            .build();

        try (var httpclient = HttpClients.custom()
            .setUserAgent(userAgent)
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .build()) {

            HttpGet request = new HttpGet(uri);
            if (GITHUB_RELEASES.getHost().equals(uri.getHost())) {
                request.addHeader("Accept", "application/vnd.github+json");
                request.addHeader("X-GitHub-Api-Version", "2026-03-10");
            }
            return httpclient.execute(request, response -> {
                HttpEntity entity = response.getEntity();
                String body = entity != null ? EntityUtils.toString(entity) : null;
                return parseCheckResult(response.getCode(), body, getCurrentVersion());
            });
        }
    }

    /**
     * Reads a GitHub release list, or the version of a version feed ({@code version}).
     *
     * @return The result, or NULL for a non-2xx status (e.g. GitHub's rate limit) or an unexpected body.
     */
    static @Nullable CheckResult parseCheckResult(int status, @Nullable String body, @NotNull String currentVersion) {
        if (status < 200 || status >= 300 || body == null) {
            return null;
        }

        JsonElement json;
        try {
            json = JsonParser.parseString(body);
        } catch (JsonParseException ex) {
            return null;
        }
        if (json.isJsonArray()) {
            return newestReleases(json.getAsJsonArray(), currentVersion);
        }
        if (json.isJsonObject() && json.getAsJsonObject().has("version")) {
            return new CheckResult(json.getAsJsonObject().get("version").getAsString(), null);
        }
        return null;
    }

    /**
     * Picks the latest release on the running major version, and the latest release overall if its major
     * version is higher. Drafts, prereleases and tags that aren't a version are skipped.
     */
    private static @NotNull CheckResult newestReleases(@NotNull JsonArray list, @NotNull String currentVersion) {
        Integer currentMajor = majorVersion(currentVersion);
        List<Release> releases = list.asList().stream()
            .map(SkyUpdateChecker::publishedRelease)
            .filter(Objects::nonNull)
            .toList();
        Comparator<Release> byVersion = Comparator.comparing(release -> new ComparableVersion(release.version()));

        String latestVersion = releases.stream()
            .filter(release -> Objects.equals(majorVersion(release.version()), currentMajor))
            .max(byVersion)
            .map(Release::version)
            .orElse(null);
        Release newMajorRelease = releases.stream()
            .max(byVersion)
            .filter(release -> currentMajor != null && majorVersion(release.version()) > currentMajor)
            .orElse(null);
        return new CheckResult(latestVersion, newMajorRelease);
    }

    /**
     * @return The release, or NULL for a draft, a prerelease or an entry without a version tag.
     */
    private static @Nullable Release publishedRelease(@NotNull JsonElement element) {
        if (!element.isJsonObject()) {
            return null;
        }
        JsonObject release = element.getAsJsonObject();
        String tag = stringMember(release, "tag_name");
        String url = stringMember(release, "html_url");
        if (isTrue(release, "draft") || isTrue(release, "prerelease") || tag == null || url == null) {
            return null;
        }
        String version = tag.startsWith("v") ? tag.substring(1) : tag;
        return majorVersion(version) != null ? new Release(version, url) : null;
    }

    private static @Nullable String stringMember(@NotNull JsonObject obj, @NotNull String name) {
        JsonElement member = obj.get(name);
        return member != null && member.isJsonPrimitive() && member.getAsJsonPrimitive().isString()
            ? member.getAsString() : null;
    }

    private static boolean isTrue(@NotNull JsonObject obj, @NotNull String name) {
        JsonElement member = obj.get(name);
        return member != null && member.isJsonPrimitive() && member.getAsJsonPrimitive().isBoolean()
            && member.getAsBoolean();
    }

    /**
     * @return The major version, or NULL if the version doesn't start with major.minor.
     */
    private static @Nullable Integer majorVersion(@NotNull String version) {
        Matcher matcher = VERSION.matcher(version);
        return matcher.matches() ? Integer.valueOf(matcher.group(1)) : null;
    }

    public boolean isNewerVersion(String currentVersion, String newVersion) {
        ComparableVersion current = new ComparableVersion(currentVersion);
        ComparableVersion target = new ComparableVersion(newVersion);
        return target.compareTo(current) > 0;
    }
}
