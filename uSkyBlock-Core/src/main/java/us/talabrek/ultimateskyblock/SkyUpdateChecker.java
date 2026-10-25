package us.talabrek.ultimateskyblock;

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
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class SkyUpdateChecker implements UpdateChecker {

    // Skips drafts and prereleases, so a withdrawn release can be hidden by turning it into either.
    private static final URI GITHUB_LATEST_RELEASE =
        URI.create("https://api.github.com/repos/uskyblock/uSkyBlock/releases/latest");

    private final Logger logger;
    private final RuntimeConfigs runtimeConfigs;
    private final uSkyBlock plugin;

    private String latestVersion;
    private volatile boolean lastCheckFailed;

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
     * Triggers an update of the latest version info (the latest GitHub release, or the staging feed on the
     * STAGING branch), and will log an INFO message if an update is available or the check failed.
     */
    public void checkForUpdates() {
        URI uri = updateUri(runtimeConfigs.current().pluginUpdates().branch());

        fetchLatestVersion(uri).thenAccept(this::handleCheckResult);
    }

    void handleCheckResult(@Nullable String version) {
        boolean failedBefore = lastCheckFailed;
        latestVersion = version;
        lastCheckFailed = version == null;
        // Only the first failure in a row is logged, so a lasting outage doesn't repeat it on every check.
        if (lastCheckFailed && !failedBefore) {
            logger.info("Failed to check for new uSkyBlock versions. "
                + "Please check for updates manually at https://uskyblock.github.io/");
        }

        if (isUpdateAvailable()) {
            logger.info("There is a new version of uSkyBlock available: " + getLatestVersion());
            logger.info("Visit https://uskyblock.github.io/ to download.");
        }
    }

    /**
     * @return True if the most recent update check got no version info, so admins have to check manually.
     */
    public boolean lastCheckFailed() {
        return lastCheckFailed;
    }

    static @NotNull URI updateUri(@NotNull String branch) {
        return branch.equalsIgnoreCase("STAGING") ? URL_STAGING : GITHUB_LATEST_RELEASE;
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
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeAsync(() -> {
            try {
                return requestLatestVersion(uri);
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

    @Nullable String requestLatestVersion(@NotNull URI uri) throws IOException {
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
            if (GITHUB_LATEST_RELEASE.getHost().equals(uri.getHost())) {
                request.addHeader("Accept", "application/vnd.github+json");
                request.addHeader("X-GitHub-Api-Version", "2026-03-10");
            }
            return httpclient.execute(request, response -> {
                HttpEntity entity = response.getEntity();
                String body = entity != null ? EntityUtils.toString(entity) : null;
                return parseLatestVersion(response.getCode(), body);
            });
        }
    }

    /**
     * Reads the version from a GitHub release ({@code tag_name}, without a leading "v") or from a version
     * feed ({@code version}).
     *
     * @return The version, or NULL for a non-2xx status (e.g. GitHub's rate limit) or an unexpected body.
     */
    static @Nullable String parseLatestVersion(int status, @Nullable String body) {
        if (status < 200 || status >= 300 || body == null) {
            return null;
        }

        JsonElement json;
        try {
            json = JsonParser.parseString(body);
        } catch (JsonParseException ex) {
            return null;
        }
        if (!json.isJsonObject()) {
            return null;
        }

        JsonObject obj = json.getAsJsonObject();
        if (obj.has("tag_name")) {
            String tag = obj.get("tag_name").getAsString();
            return tag.startsWith("v") ? tag.substring(1) : tag;
        }
        if (obj.has("version")) {
            return obj.get("version").getAsString();
        }
        return null;
    }

    public boolean isNewerVersion(String currentVersion, String newVersion) {
        ComparableVersion current = new ComparableVersion(currentVersion);
        ComparableVersion target = new ComparableVersion(newVersion);
        return target.compareTo(current) > 0;
    }
}
