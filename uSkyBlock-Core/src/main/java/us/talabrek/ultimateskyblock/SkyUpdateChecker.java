package us.talabrek.ultimateskyblock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
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

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SkyUpdateChecker implements UpdateChecker {

    // Skips drafts and prereleases, so a withdrawn release can be hidden by turning it into either.
    private static final URI GITHUB_LATEST_RELEASE =
        URI.create("https://api.github.com/repos/uskyblock/uSkyBlock/releases/latest");

    private final Logger logger;
    private final RuntimeConfigs runtimeConfigs;
    private final uSkyBlock plugin;

    private String latestVersion;

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
     * STAGING branch), and will log an INFO message if an update is available.
     */
    public void checkForUpdates() {
        URI uri = GITHUB_LATEST_RELEASE;

        if (runtimeConfigs.current().pluginUpdates().branch().equalsIgnoreCase("STAGING")) {
            uri = URL_STAGING;
        }

        fetchLatestVersion(uri).thenAccept(version -> {
            latestVersion = version;
            if (latestVersion == null) {
                logger.info("Failed to check for new uSkyBlock versions.");
            }

            if (isUpdateAvailable()) {
                logger.info("There is a new version of uSkyBlock available: " + getLatestVersion());
                logger.info("Visit https://uskyblock.github.io/ to download.");
            }
        });
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
            } catch (Exception ex) {
                // This runs asynchronously and can still be in flight when the server shuts down or
                // restarts, tearing down the plugin classloader mid-request. Such a failure is expected
                // and not actionable, so only surface it while the plugin is actually enabled.
                if (plugin.isEnabled()) {
                    logger.log(Level.SEVERE, "Exception while trying to fetch latest plugin version.", ex);
                }
            }

            return null;
        });

        return future;
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
