package us.talabrek.ultimateskyblock;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
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

    private final Logger logger;
    private final RuntimeConfigs runtimeConfigs;
    private final uSkyBlock plugin;

    private String latestVersion;

    private final Gson gson = new Gson();

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
     * Triggers an update of the latest version info from the uSkyBlock website, and will log an INFO message if
     * an update is available.
     */
    public void checkForUpdates() {
        URI uri = URL_RELEASE;

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
                logger.info("Visit https://www.uskyblock.ovh/get to download.");
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
                return httpclient.execute(request, response -> {
                    int status = response.getCode();
                    if (status < 200 || status >= 300) {
                        return null;
                    }

                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        JsonObject obj = gson.fromJson(EntityUtils.toString(entity), JsonObject.class);
                        if (obj.has("version")) {
                            return obj.get("version").getAsString();
                        }
                    }
                    return null;
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

    public boolean isNewerVersion(String currentVersion, String newVersion) {
        ComparableVersion current = new ComparableVersion(currentVersion);
        ComparableVersion target = new ComparableVersion(newVersion);
        return target.compareTo(current) > 0;
    }
}
