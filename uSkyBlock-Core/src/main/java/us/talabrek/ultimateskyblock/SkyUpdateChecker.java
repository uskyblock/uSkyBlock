package us.talabrek.ultimateskyblock;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.api.plugin.UpdateChecker;
import us.talabrek.ultimateskyblock.bootstrap.PluginLog;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.update.ModrinthClient;
import us.talabrek.ultimateskyblock.update.ModrinthVersion;
import us.talabrek.ultimateskyblock.update.ServerPlatform;
import us.talabrek.ultimateskyblock.update.UpdateStager;
import us.talabrek.ultimateskyblock.update.Updates;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Checks Modrinth for newer uSkyBlock versions.
 */
@Singleton
public class SkyUpdateChecker implements UpdateChecker {

    public static final URI UPGRADE_GUIDE = URI.create("https://uskyblock.org");

    private final Logger logger;
    private final RuntimeConfigs runtimeConfigs;
    private final uSkyBlock plugin;
    private final ModrinthClient modrinth;
    private final UpdateStager stager;
    private final ServerPlatform platform;

    private volatile @Nullable Updates updates;
    private volatile boolean lastCheckFailed;

    private interface Request<T> {
        @Nullable T send() throws IOException;
    }

    @Inject
    public SkyUpdateChecker(
        @NotNull uSkyBlock plugin,
        @NotNull RuntimeConfigs runtimeConfigs,
        @NotNull @PluginLog Logger logger,
        @NotNull ModrinthClient modrinth,
        @NotNull UpdateStager stager,
        @NotNull ServerPlatform platform
    ) {
        this.plugin = plugin;
        this.runtimeConfigs = runtimeConfigs;
        this.logger = logger;
        this.modrinth = modrinth;
        this.stager = stager;
        this.platform = platform;
    }

    /**
     * Triggers an update of the latest version info, and will log an INFO message if an update or a new major version
     * is available or the check failed. With {@code plugin-updates.auto-stage}, stages the update once it is old
     * enough.
     */
    public @NotNull CompletableFuture<Void> checkForUpdates() {
        return fetchUpdates().thenAccept(result -> {
            handleCheckResult(result);
            RuntimeConfig.PluginUpdates config = runtimeConfigs.current().pluginUpdates();
            if (result != null && config.autoStage()) {
                stager.autoStage(result, config.autoStageDelay());
            }
        });
    }

    void handleCheckResult(@Nullable Updates result) {
        boolean failedBefore = lastCheckFailed;
        updates = result;
        lastCheckFailed = result == null;
        // Only the first failure in a row is logged, so a lasting outage doesn't repeat it on every check.
        if (lastCheckFailed && !failedBefore) {
            logger.info("Failed to check for new uSkyBlock versions. "
                + "Please check for updates manually at " + ModrinthVersion.PROJECT_PAGE);
        }
        if (result == null) {
            return;
        }

        ModrinthVersion update = result.update();
        if (update != null) {
            logger.info("There is a new version of uSkyBlock available: " + update.version());
            logger.info("Run /usb update to stage it for the next restart, or download it from " + update.page());
        }
        ModrinthVersion newMajor = result.newMajor();
        if (newMajor != null) {
            logger.info("There is a new major version of uSkyBlock available: " + newMajor.version()
                + " (needs Minecraft " + newMajor.minecraftVersions() + ")");
            logger.info("Read the upgrade guide before upgrading: " + UPGRADE_GUIDE);
            logger.info("Release notes: " + newMajor.page());
        }
    }

    /**
     * @return True if the most recent update check got no version info, so admins have to check manually.
     */
    public boolean lastCheckFailed() {
        return lastCheckFailed;
    }

    /**
     * @return What the most recent update check found, NULL before the first check and after a failed one.
     */
    public @Nullable Updates getUpdates() {
        return updates;
    }

    /**
     * @return The newest version with a higher major version than the running one, NULL if there is none. It is
     * announced apart from regular updates, as upgrading to it may need more than replacing the jar.
     */
    public @Nullable ModrinthVersion getNewMajorVersion() {
        Updates current = updates;
        return current != null ? current.newMajor() : null;
    }

    @Override
    public boolean isUpdateAvailable() {
        Updates current = updates;
        return current != null && current.update() != null;
    }

    @Override
    public @Nullable String getLatestVersion() {
        Updates current = updates;
        ModrinthVersion latest = current != null ? current.latestOnMajor() : null;
        return latest != null ? latest.version() : null;
    }

    @Override
    public @NotNull String getCurrentVersion() {
        return plugin.getDescription().getVersion();
    }

    /**
     * Asks Modrinth for the versions on the configured update channel, off the server thread.
     *
     * @return The versions, NULL if Modrinth can't be reached or answers with something unexpected.
     */
    public @NotNull CompletableFuture<@Nullable Updates> fetchUpdates() {
        String branch = runtimeConfigs.current().pluginUpdates().branch();
        return fetch(() -> {
            List<ModrinthVersion> versions = ModrinthVersion.parseList(modrinth.body(modrinth.versionsUri()));
            return versions != null ? Updates.of(versions, branch, getCurrentVersion(), platform) : null;
        });
    }

    @Override
    public CompletableFuture<String> fetchLatestVersion(URI uri) {
        String branch = runtimeConfigs.current().pluginUpdates().branch();
        return fetch(() -> parseLatestVersion(modrinth.body(uri), branch, getCurrentVersion(), platform));
    }

    /**
     * Reads a Modrinth version list, or a version feed such as {@link UpdateChecker#URL_STAGING}.
     *
     * @return The latest version on the running major version that runs on the server, or the feed's version; NULL
     * for an unexpected body.
     */
    static @Nullable String parseLatestVersion(
        @NotNull String body,
        @NotNull String branch,
        @NotNull String currentVersion,
        @NotNull ServerPlatform platform
    ) {
        List<ModrinthVersion> versions = ModrinthVersion.parseList(body);
        if (versions != null) {
            ModrinthVersion latest = Updates.of(versions, branch, currentVersion, platform).latestOnMajor();
            return latest != null ? latest.version() : null;
        }
        try {
            JsonElement json = JsonParser.parseString(body);
            JsonElement version = json.isJsonObject() ? json.getAsJsonObject().get("version") : null;
            return version != null && version.isJsonPrimitive() ? version.getAsString() : null;
        } catch (JsonParseException ex) {
            return null;
        }
    }

    private <T> @NotNull CompletableFuture<@Nullable T> fetch(@NotNull Request<T> request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return request.send();
            } catch (IOException ex) {
                // Modrinth can't be reached (no network, DNS, timeout, TLS) or answers with an error status, e.g. its
                // rate limit. That is expected on some hosts and not a plugin bug, so nothing is logged here;
                // handleCheckResult logs the manual-check hint.
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
    }

    @Override
    public boolean isNewerVersion(String currentVersion, String newVersion) {
        return Updates.isNewer(currentVersion, newVersion);
    }
}
