package us.talabrek.ultimateskyblock.update;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Reads uSkyBlock's versions from the Modrinth API and downloads their files.
 */
@Singleton
public class ModrinthClient {

    private static final URI API = URI.create("https://api.modrinth.com/");
    private static final Duration DEFAULT_RATE_LIMIT_WAIT = Duration.ofMinutes(1);
    private static final Timeout TIMEOUT = Timeout.ofSeconds(10);

    private final URI api;
    private final String userAgent;
    private final Clock clock;
    private volatile Instant rateLimitedUntil = Instant.MIN;

    /**
     * Thrown when Modrinth answers with a status that isn't a success, or when the client holds back a request
     * while Modrinth's rate limit is reached.
     */
    public static class UnexpectedResponseException extends IOException {
        public UnexpectedResponseException(@NotNull String message) {
            super(message);
        }
    }

    @Inject
    public ModrinthClient(@NotNull uSkyBlock plugin, @NotNull Clock clock) {
        this(API, plugin.getDescription().getVersion(), clock);
    }

    ModrinthClient(@NotNull URI api, @NotNull String pluginVersion, @NotNull Clock clock) {
        this.api = api;
        this.userAgent = "uskyblock/uSkyBlock/" + pluginVersion + " (https://github.com/uskyblock/uSkyBlock)";
        this.clock = clock;
    }

    /**
     * @return The API request that lists all of uSkyBlock's versions.
     */
    public @NotNull URI versionsUri() {
        return api.resolve("v2/project/" + ModrinthVersion.PROJECT + "/version?include_changelog=false");
    }

    /**
     * Requests a body, e.g. from {@link #versionsUri()}. Blocks; don't call it on the server thread.
     */
    public @NotNull String body(@NotNull URI uri) throws IOException {
        return get(uri, response -> {
            HttpEntity entity = response.getEntity();
            return entity != null ? EntityUtils.toString(entity) : "";
        });
    }

    /**
     * Downloads a version's file. Blocks; don't call it on the server thread.
     */
    public void download(@NotNull URI file, @NotNull OutputStream out) throws IOException {
        get(file, response -> {
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                throw new UnexpectedResponseException("No file at " + file);
            }
            entity.writeTo(out);
            return null;
        });
    }

    private interface ResponseReader<T> {
        @Nullable T read(@NotNull ClassicHttpResponse response) throws IOException, HttpException;
    }

    private <T> T get(@NotNull URI uri, @NotNull ResponseReader<T> reader) throws IOException {
        if (clock.instant().isBefore(rateLimitedUntil)) {
            throw new UnexpectedResponseException("Modrinth's rate limit is reached until " + rateLimitedUntil);
        }
        try (CloseableHttpClient httpclient = httpClient()) {
            return httpclient.execute(new HttpGet(uri), response -> {
                int status = response.getCode();
                if (status == 429) {
                    rateLimitedUntil = clock.instant().plus(rateLimitWait(response));
                }
                if (status < 200 || status >= 300) {
                    throw new UnexpectedResponseException("Modrinth answered " + status + " for " + uri);
                }
                return reader.read(response);
            });
        }
    }

    // Modrinth sends the seconds until its rate limit resets.
    private static @NotNull Duration rateLimitWait(@NotNull ClassicHttpResponse response) {
        Header reset = response.getFirstHeader("X-Ratelimit-Reset");
        if (reset != null) {
            try {
                return Duration.ofSeconds(Math.max(1, Long.parseLong(reset.getValue().trim())));
            } catch (NumberFormatException ignored) {
                // Falls back to the default wait.
            }
        }
        return DEFAULT_RATE_LIMIT_WAIT;
    }

    private @NotNull CloseableHttpClient httpClient() {
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
            .setConnectTimeout(TIMEOUT)
            .setSocketTimeout(TIMEOUT)
            .build();
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(TIMEOUT)
            .build();
        return HttpClients.custom()
            .setUserAgent(userAgent)
            .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionConfig)
                .build())
            .setDefaultRequestConfig(requestConfig)
            // A retry would ignore Modrinth's rate limit; the next scheduled check is the retry.
            .disableAutomaticRetries()
            .build();
    }
}
